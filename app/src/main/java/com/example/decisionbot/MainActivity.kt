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
import androidx.compose.ui.Alignment
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
import com.example.decisionbot.repository.AppDao
import com.example.decisionbot.repository.AppDatabase
import com.example.decisionbot.repository.AppRepository
import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice
import com.example.decisionbot.repository.entity.RequirementBox
import com.example.decisionbot.repository.entity.Result
import com.example.decisionbot.ui.theme.DecisionBotTheme
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.manualcomposablecalls.composable
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
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
                    MainComponent(AppRepository(dao))
                }
            }
        }
    }
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
fun MainComponent(repo: AppRepository) {
    DestinationsNavHost(navGraph = NavGraphs.root) {
        composable(HomePageDestination) {
            HomePage(
                { destinationsNavigator.navigate(ChoiceListPageDestination) },
                { destinationsNavigator.navigate(AnswerQuestionPageDestination) }
            )
        }
        composable(EditChoicePageDestination) {
            EditChoicePage(object: EditChoicePageViewModel(navArgs.choice) {

                override fun updateChoicePrompt(prompt: String) {
                    viewModelScope.launch {
                        val updatedChoice = choice.value.copy(prompt = prompt)
                        repo.editChoice(updatedChoice)
                        choiceMut.value = updatedChoice
                    }
                }

                override fun getAnswers(): List<ListItem<Answer>> {
                    viewModelScope.launch {
                        answersMut.value = repo.getAnswersForChoice(choice.value)
                    }
                    return makeListItems(
                        answersMut,
                        { new -> viewModelScope.launch { repo.editAnswer(new) } },
                        { item -> viewModelScope.launch { repo.deleteAnswer(item) } }
                    )
                }

                override fun getRequirements(): List<ListItem<RequirementBox>> {
                    viewModelScope.launch {
                        requirementsMut.value = repo.getRequirementBoxInfoFor(choice.value)
                    }
                    return makeListItems(
                        requirementsMut,
                        { new -> viewModelScope.launch { repo.editRequirementBox(new) } },
                        { item -> viewModelScope.launch { repo.deleteRequirementBox(item) }}
                    )
                }
            })
        }
        composable(ChoiceListPageDestination) {
            ChoiceListPage(makeChoiceListViewModel(repo, destinationsNavigator))
        }
        composable(AnswerQuestionPageDestination) {
            AnswerQuestionPage(object: AnswerQuestionPageViewModel() {
                override fun setupNextQuestion() {
                    viewModelScope.launch {
                        val result = repo.getNextChoiceForDecision()
                        if (result != null) {
                            choiceMut.value = result
                            answersMut.value = repo.getAnswersForChoice(result)
                        }
                    }
                }

                override fun getResults(): State<List<Result>> {
                    viewModelScope.launch {
                        resultsMut.value = repo.getResults()
                    }
                    return resultsMut
                }

            })
        }
    }
}

fun makeChoiceListViewModel(repo: AppRepository, nav: DestinationsNavigator): ChoiceListViewModel {
   return object: ChoiceListViewModel() {
       override fun fillWithCurrentChoices() {
           viewModelScope.launch {
               choicesMut.value = repo.getAllChoices()
           }
       }

       override fun insertNewChoice(prompt: String) {
           viewModelScope.launch {
               choicesMut.value = choices.value.plus(repo.insertChoice(prompt))
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
    protected val choiceMut: MutableState<Choice?> = mutableStateOf(null)
    protected val answersMut: MutableState<List<Answer>> = mutableStateOf(emptyList())
    protected val resultsMut: MutableState<List<Result>> = mutableStateOf(emptyList())

    val choice: State<Choice?> = choiceMut
    val answers: State<List<Answer>> = answersMut

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

abstract class EditChoicePageViewModel(choice: Choice) : ViewModel() {
    protected val choiceMut: MutableState<Choice> = mutableStateOf(choice)
    protected val answersMut: MutableState<List<Answer>> = mutableStateOf(emptyList())
    protected val requirementsMut: MutableState<List<RequirementBox>> = mutableStateOf(emptyList())

    val choice: State<Choice> = choiceMut

    abstract fun updateChoicePrompt(prompt: String)
    abstract fun getAnswers(): List<ListItem<Answer>>
    abstract fun getRequirements(): List<ListItem<RequirementBox>>
}

fun <T> makeListItems(
    items: MutableState<List<T>>,
    edit: (T) -> Unit,
    delete: (T) -> Unit
): List<ListItem<T>> {
    return items.value.map { t ->
        val st = mutableStateOf(t)
        ListItem(
            get = { st.value },
            edit = { new ->
                edit(new)
                st.value = new
            },
            delete = {
                delete(t)
                items.value = items.value.minus(t)
            }
        )
    }.toList()
}

data class EditChoicePageNavArgs(
    val choice: Choice
)

@Destination(navArgsDelegate = EditChoicePageNavArgs::class)
@Composable
fun EditChoicePage(
    st: EditChoicePageViewModel
) {
  Column {
      Text(text = "Prompt", fontSize = 25.sp)
      TextField(
          value = st.choice.value.prompt,
          onValueChange = { st.updateChoicePrompt(it) }
      )

      Text(text = "Answers", fontSize = 25.sp)
      EditDeleteBoxList(st.getAnswers()) { answer ->
          Text(text = answer.description)
      }

      Text(text = "Requirements", fontSize = 25.sp)
      EditDeleteBoxList(st.getRequirements()) { requirement ->
          Text(text = requirement.prompt)
          Text(text = requirement.description)
      }
  }
}

data class ListItem<T>(
    val edit: (T) -> Unit,
    val delete: () -> Unit,
    val get: () -> T
)

@Composable
fun <Item> EditDeleteBoxList(
    items: List<ListItem<Item>>,
    displayItem: @Composable (Item) -> Unit,
) {
    LazyColumn {
        items(items) { item ->
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    displayItem(item.get())
                    TextButton(onClick = { }){
                        Text(text = "edit")
                    }
                    TextButton(onClick = { item.delete() }) {
                        Text(text = "delete")
                    }
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
