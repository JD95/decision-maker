package com.example.decisionbot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.decisionbot.destinations.AnswerQuestionPageDestination
import com.example.decisionbot.destinations.ChoiceListPageDestination
import com.example.decisionbot.destinations.EditChoicePageDestination
import com.example.decisionbot.destinations.HomePageDestination
import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice
import com.example.decisionbot.repository.entity.RequirementBox
import com.example.decisionbot.repository.entity.Result
import com.example.decisionbot.ui.theme.DecisionBotTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "data.db")
            .build()
        val dao = db.dao()
        Log.i("startup", "Starting up app!")
        this.lifecycleScope.launch {
            seedTestDb(dao)
        }
        setContent {
            DecisionBotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainComponent(dao)
                }
            }
        }
    }
}

data class Repo(
    val dbGetAllChoices: suspend (MutableState<List<Choice>>) -> Unit,
    val insertChoice: suspend (String, MutableState<List<Choice>>) -> Unit,
    val choiceCrud: Crud<Long, Choice>,
    val answerCrud: Crud<Choice, Answer>,
    val requirementBoxCrud: Crud<Choice, RequirementBox>,
)

data class Crud<Key, T>(
    val getAssociated: @Composable (Key) -> State<List<T>>,
    val edit: (T) -> Unit,
    val delete: (T) -> Unit
)

fun makeRepo (
    dao: AppDao,
    scope: CoroutineScope
) : Repo {
    return Repo(
        dbGetAllChoices = { value ->
            Log.i("repo", "Getting all choices")
            val result = dao.getAllChoices()
            Log.i("repo", "There are ${result.size} choices!")
            value.value = result
        },
        insertChoice = { prompt, st ->
            val id = dao.insertChoice(prompt)
            st.value = st.value.plus(Choice(id, prompt))
        },
        choiceCrud = Crud(
            getAssociated = { id ->
                produceState(emptyList()) {
                    value = listOf(dao.getChoice(id))
                }
            },
            edit = { x -> scope.launch { dao.updateChoice(x.id, x.prompt) }},
            delete = { x -> scope.launch { dao.deleteChoice(x.id) } }
        ),
        answerCrud = Crud(
            getAssociated = { choice ->
                produceState(emptyList()) {
                    value = dao.getAnswersFor(choice.id)
                }
            },
            edit = { x -> scope.launch { dao.updateAnswer(x.id, x.description) } },
            delete = { x -> scope.launch { dao.deleteAnswer(x.id) }}
        ),
        requirementBoxCrud = Crud(
            getAssociated = { x ->
                produceState(emptyList()) {
                    value = dao.getRequirementBoxFor(x.id)
                }
            },
            edit = { x -> scope.launch { dao.updateRequirement(x.id, x.choice, x.answer) } },
            delete = { x -> scope.launch { dao.deleteRequirement(x.id) } }
        ),
    )
}

suspend fun seedTestDb(dao: AppDao) {
    Log.i("startup", "Seeding database!")
    val stayInGoOut = dao.insertChoice("Stay in or go out?")
    val stayIn = dao.insertAnswer(stayInGoOut, "Stay in")
    val goOut = dao.insertAnswer(stayInGoOut, "Go out")

    val whichBar = dao.insertChoice("Which bar?")
    dao.insertAnswer(whichBar, "Winston Whiskey")
    dao.insertAnswer(whichBar, "Tequila Turnaround")
    dao.insertRequirement(whichBar, goOut)

    val movieOrAnime = dao.insertChoice("Watch a movie or anime?")
    dao.insertAnswer(movieOrAnime, "Movie")
    dao.insertAnswer(movieOrAnime, "Anime")
    dao.insertRequirement(stayInGoOut, stayIn)
}

@Composable
fun MainComponent(dao: AppDao) {
    val scope = rememberCoroutineScope()
    DestinationsNavHost(navGraph = NavGraphs.root) {
        composable(HomePageDestination) {
            HomePage(
                { destinationsNavigator.navigate(ChoiceListPageDestination) },
                { destinationsNavigator.navigate(AnswerQuestionPageDestination) }
            )
        }
        composable(EditChoicePageDestination) {
            val env = makeRepo(dao, scope)
            EditChoicePage(navArgs.choice, env)
        }
        composable(ChoiceListPageDestination) {
            ChoiceListPage(makeChoiceListViewModel(dao, destinationsNavigator))
        }
        composable(AnswerQuestionPageDestination) {
            AnswerQuestionPage(object: AnswerQuestionPageViewModel() {
                override fun setupNextQuestion() {
                    viewModelScope.launch {
                        val results = dao.getNextChoice()
                        if (results.isNotEmpty()) {
                            val x = results[0]
                            _choice.value = x
                            _answers.value = dao.getAnswersFor(x.id)
                        }
                    }
                }

                override fun getResults(): State<List<Result>> {
                    viewModelScope.launch {
                        _results.value = dao.getResults()
                    }
                    return _results
                }

            })
        }
    }
}

fun makeChoiceListViewModel(dao: AppDao, nav: DestinationsNavigator): ChoiceListViewModel {
   return object: ChoiceListViewModel() {
       override fun fillWithCurrentChoices() {
           viewModelScope.launch {
               choicesMut.value = dao.getAllChoices()
           }
       }

       override fun insertNewChoice(prompt: String) {
           viewModelScope.launch {
               val id = dao.insertChoice(prompt)
               choicesMut.value = choices.value.plus(Choice(id, prompt))
           }
       }

       override fun gotoEditChoicePage(choice: Choice) {
           nav.navigate(EditChoicePageDestination(choice))
       }
   }
}

@Composable
@Destination(start = true)
fun HomePage(
    gotoChoiceListPage: () -> Unit,
    gotoDecisionPage: () -> Unit
) {
    Column {
        Button(onClick = { gotoChoiceListPage() }) {
            Text(text = "Edit Choices")
        }
        Button(onClick = { gotoDecisionPage() }) {
            Text(text = "Make Decision")
        }
    }
}

abstract class ChoiceListViewModel : ViewModel()  {
    protected val choicesMut = mutableStateOf(emptyList<Choice>())
    val choices: State<List<Choice>> = choicesMut
    abstract fun fillWithCurrentChoices()
    abstract fun insertNewChoice(prompt: String)
    abstract fun gotoEditChoicePage(choice: Choice)
}

@Composable
@Destination
fun ChoiceListPage(st: ChoiceListViewModel)  {
    st.fillWithCurrentChoices()
    Column {
        Text(text = "Choices", fontSize = 25.sp)

        LazyColumn {
            items(st.choices.value) { choice ->
                Button(
                    onClick = { st.gotoEditChoicePage(choice) }
                ) {
                    Text(text = choice.prompt)
                }
            }
        }

        Button(onClick = { st.insertNewChoice("New Choice!") }) {
            Text(text = "Add Choice")
        }
    }
}

abstract class AnswerQuestionPageViewModel : ViewModel() {
    protected val _choice: MutableState<Choice?> = mutableStateOf(null)
    protected val _answers: MutableState<List<Answer>> = mutableStateOf(emptyList())
    protected val _results: MutableState<List<Result>> = mutableStateOf(emptyList())

    val choice: State<Choice?> = _choice
    val answers: State<List<Answer>> = _answers

    abstract fun setupNextQuestion()
    abstract fun getResults(): State<List<Result>>
}

@Composable
@Destination
fun AnswerQuestionPage(st: AnswerQuestionPageViewModel) {
    st.setupNextQuestion()
    Column {
        Text(text = "Decision Bot", fontSize = 30.sp)
        if (st.choice.value != null) {
            ChoiceForm(st.choice.value!!, st.answers.value)
        } else {
            ResultsList(st.getResults().value)
        }
    }
}

@Destination
@Composable
fun EditChoicePage(
    choice: Choice,
    env: Repo,
) {
  Column {
      Text(text = "Prompt", fontSize = 25.sp)
      TextField(
          value = choice.prompt,
          onValueChange = { it: String -> env.choiceCrud.edit(choice.copy(prompt = it)) }
      )

      Text(text = "Answers", fontSize = 25.sp)
      EditDeleteBoxList(choice, env.answerCrud) { answer ->
          Text(text = answer.description)
      }

      Text(text = "Requirements", fontSize = 25.sp)
      EditDeleteBoxList(choice, env.requirementBoxCrud) { requirement ->
          Text(text = requirement.prompt)
          Text(text = requirement.description)
      }
  }
}

@Composable
fun <Key, Item> EditDeleteBoxList(
    choice: Key,
    updater: Crud<Key, Item>,
    displayItem: @Composable (Item) -> Unit,
) {
    val stuff = updater.getAssociated(choice).value
    LazyColumn {
        items(stuff) { item ->
            Box {
                displayItem(item)
                TextButton(onClick = { updater.edit(item) }) {
                    Text(text = "edit")
                }
                TextButton(onClick = { updater.delete(item) }) {
                    Text(text = "delete")
                }
            }
        }
    }
}

@Composable
fun ChoiceForm(choice: Choice, answers: List<Answer>) {
    Text(text = choice.prompt)
    Column {
        Text("Options:")
        LazyColumn {
            items(answers) { answer ->
                AnswerField(answer)
            }
        }
    }
}

@Composable
fun AnswerField(answer: Answer) {
   Text(text = answer.description)
}

@Composable
fun ResultsList(decisions: List<Result>) {
    Text(text = "Your decision has been made!", fontSize = 25.sp)
    Column {
        Text("Decisions:", fontSize = 20.sp)
        LazyColumn {
            items(decisions) { result ->
                ResultDisplay(result)
            }
        }
    }
}

@Composable
fun ResultDisplay(result: Result) {
    Column {
        Text(text = result.prompt)
        Text(text = result.answer)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    DecisionBotTheme {
        Text(text = "A decision bot")
    }
}
