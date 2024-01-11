package com.example.decisionbot

import android.os.Bundle
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
import androidx.room.Room
import com.example.decisionbot.destinations.ChoiceListPageDestination
import com.example.decisionbot.destinations.EditChoicePageDestination
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

        setContent {
            DecisionBotTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    LaunchedEffect(Unit) {
                        seedTestDb(dao)
                    }
                    MainComponent(dao)
                }
            }
        }
    }
}

fun mainEnv(dao: AppDao, navigator: DestinationsNavigator, scope: CoroutineScope) : MainEnv {
    return MainEnv(
        navigator = navigator,
        updateChoice = { choice ->
            scope.launch {
                dao.updateChoice(choice.id, choice.prompt)
            }
        },
        getAnswersFor = { x -> emptyList() },
        editAnswer = { x -> Unit },
        deleteAnswer = { x -> Unit },
        getRequirementsFor = { x -> emptyList() },
        editRequirement = { x -> Unit },
        deleteRequirement = { x -> Unit }
    )
}

suspend fun seedTestDb(dao: AppDao) {
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
data class MainEnv(
    val navigator: DestinationsNavigator,
    val updateChoice: (Choice) -> Unit,
    val getAnswersFor: (Choice) -> List<Answer>,
    val editAnswer: (Answer) -> Unit,
    val deleteAnswer: (Answer) -> Unit,
    val getRequirementsFor: (Choice) -> List<RequirementBox>,
    val editRequirement: (RequirementBox) -> Unit,
    val deleteRequirement: (RequirementBox) -> Unit,
)
@Composable
fun MainComponent(dao: AppDao) {
    val scope = rememberCoroutineScope()
    DestinationsNavHost(navGraph = NavGraphs.root) {
        composable(EditChoicePageDestination) {
            EditChoicePage(navArgs.choice, mainEnv(dao, destinationsNavigator, scope))
        }
    }
}
@Composable
@Destination(start = true)
fun HomePage(navigator: DestinationsNavigator) {
    Column {
        Button(onClick = { navigator.navigate(ChoiceListPageDestination())}) {
            Text(text = "Edit Choices")
        }
        Button(onClick = { }) {
            Text(text = "Make Decision")
        }
    }
}

@Composable
@Destination
fun ChoiceListPage(
    navigator: DestinationsNavigator,
    getAllChoices: () -> List<Choice>,
    insertNewChoice: (String) -> Choice
) {
    val choices = remember { mutableStateOf(getAllChoices()) }
    val scope = rememberCoroutineScope()

    Column {
        Text(text = "Choices", fontSize = 25.sp)

        LazyColumn {
            items(choices.value) { choice ->
                Button(
                    onClick = { navigator.navigate(EditChoicePageDestination(choice)) }
                ) {
                    Text(text = choice.prompt)
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val prompt = "New Choice!"
                    choices.value = choices.value.plus(insertNewChoice(prompt))
                }
            }) {
            Text(text = "Add Choice")
        }
    }
}
@Composable
fun AnswerQuestionPage(
    navigation: MutableState<Navigation>,
    getNextChoice: () -> Choice,
    getAnswersFor: (Choice) -> List<Answer>,
    getResults: () -> List<Result>,
) {
    val choice = remember { mutableStateOf(listOf(getNextChoice())) }
    Column {
        Text(text = "Decision Bot", fontSize = 30.sp)
        if (choice.value.isNotEmpty()) {
            ChoiceForm(choice.value[0], getAnswersFor)
        } else {
            ResultsList(getResults)
        }
    }
}

@Destination
@Composable
fun EditChoicePage(
    choice: Choice,
    env: MainEnv,
) {
  Column {
      Text(text = "Prompt", fontSize = 25.sp)
      TextField(
          value = choice.prompt,
          onValueChange = { it: String -> env.updateChoice(choice.copy(prompt = it)) }
      )

      Text(text = "Answers", fontSize = 25.sp)
      EditDeleteBoxList(
          env.getAnswersFor(choice),
          env.editAnswer,
          env.deleteAnswer
      ) { answer ->
          Text(text = answer.description)
      }

      Text(text = "Requirements", fontSize = 25.sp)
      EditDeleteBoxList(
          env.getRequirementsFor(choice),
          env.editRequirement,
          env.deleteRequirement
      ) { requirement ->
          Text(text = requirement.prompt)
          Text(text = requirement.description)
      }
  }
}

@Composable
fun <Item> EditDeleteBoxList(
    items: List<Item>,
    editItem: (Item) -> Unit,
    deleteItem: (Item) -> Unit,
    displayItem: @Composable (Item) -> Unit,
) {
    LazyColumn {
        items(items) { item ->
            Box {
                displayItem(item)
                TextButton(onClick = { editItem(item) }) {
                    Text(text = "edit")
                }
                TextButton(onClick = { deleteItem(item) }) {
                    Text(text = "delete")
                }
            }
        }
    }
}
@Composable
fun ChoiceForm(choice: Choice, getAnswersFor: (Choice) -> List<Answer>) {
    val answers = remember { mutableStateOf(getAnswersFor(choice)) }
    Text(text = choice.prompt)
    Column {
        Text("Options:")
        LazyColumn {
            items(answers.value) { answer ->
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
fun ResultsList(getResults: () -> List<Result>) {
    Text(text = "Your decision has been made!", fontSize = 25.sp)
    val decisions = remember { mutableStateOf(getResults()) }
    Column {
        Text("Decisions:", fontSize = 20.sp)
        LazyColumn {
            items(decisions.value) { result ->
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
