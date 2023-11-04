package com.example.decisionbot

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.example.decisionbot.ui.theme.DecisionBotTheme
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
                    MainComponent(dao)
                }
            }
        }
    }
}
@Composable
fun MainComponent(dao: AppDao) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomePage(navController) }
        composable("choice/list") { ChoiceListPage(dao, navController) }
        composable(
            "choice/{choiceId}/edit",
            arguments = listOf(navArgument("choiceId") { type = NavType.LongType })
        ) { backStackEntry ->
            EditChoicePage(dao, backStackEntry.arguments?.getLong("choiceId")!!)
        }
        composable(
            "choice/requirement/{requirementId}/edit",
            arguments = listOf(navArgument("requirementId") { type = NavType.LongType })
        ) { backStackEntry ->
            EditRequirementPage(dao, backStackEntry.arguments?.getLong("requirementId")!!)
        }
    }
}
@Composable
fun HomePage(navController: NavController) {
    Column {
        Button(onClick = { navController.navigate("choice/list")}) {
            Text(text = "Edit Choices")
        }
        Button(onClick = { }) {
            Text(text = "Make Decision")
        }
    }
}

@Composable
fun ChoiceListPage(dao: AppDao, navController: NavController) {
    val choices = remember { mutableStateOf<List<Choice>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(choices) {
        choices.value = dao.getAllChoices()
    }

    Column {
        Text(text = "Choices", fontSize = 25.sp)

        LazyColumn {
            items(choices.value) { choice ->
                Button(
                    onClick = { navController.navigate("choice/${choice.id}/edit") }
                ) {
                    Text(text = choice.prompt)
                }
            }
        }

        Button(
            onClick = {
                scope.launch {
                    val prompt = "New Choice!"
                    val id = dao.insertChoice(prompt)
                    choices.value = choices.value.plus(Choice(id, prompt))
                }
            }) {
            Text(text = "Add Choice")
        }
    }
}

@Composable
fun EditRequirementPage(dao: AppDao, requirementId: Long) {
    WithInit({ dao.getRequirement(requirementId) }) { requirement ->

    }
}

@Composable
fun AnswerQuestionPage(dao: AppDao, navigation: MutableState<Navigation>) {
    val choice = remember { mutableStateOf(emptyList<Choice>()) }
    LaunchedEffect(choice) {
        choice.value = dao.getNextChoice();
    }
    Column {
        Text(text = "Decision Bot", fontSize = 30.sp)
        if (choice.value.isNotEmpty()) {
            ChoiceForm(dao, choice.value[0])
        } else {
            ResultsPage(dao)
        }
    }
}

@Composable
fun <S> WithInit(
    init: suspend () -> S,
    body: @Composable (MutableState<S>) -> Unit,
) {
    val slot = remember { mutableStateOf<S?>(null) }
    LaunchedEffect(slot) {
        slot.value = init()
    }
    if (slot.value != null) {
        val st = remember { mutableStateOf<S>(slot.value!!) }
        body(st)
    }
}

@Composable
fun EditChoicePage(dao: AppDao, choiceId: Long) {
    WithInit({ dao.getChoice(choiceId) }) {choice ->

        LaunchedEffect(choice) {
            dao.updateChoice(choice.value.id, choice.value.prompt)
        }

        Column {
            Text(text = "Prompt", fontSize = 25.sp)
            TextField(
                value = choice.value.prompt,
                onValueChange = { it: String -> choice.value = choice.value.copy(prompt = it) }
            )
            ListAnswerSection(dao, choice)
            ListRequirementsSection(dao, choice)
        }
    }
}

private fun <T> deleteIndexFrom(
    items: MutableState<List<T>>,
    index: Int
) {
    val mut = items.value.toMutableList()
    mut.removeAt(index)
    items.value = mut.toList()
}

@Composable
fun ListRequirementsSection(
    dao: AppDao,
    choice: MutableState<Choice>
) {

    val scope = rememberCoroutineScope()
    val requirements = remember { mutableStateOf<List<MutableState<Requirement>>>(emptyList()) }
    LaunchedEffect(requirements) {
        for(req in requirements.value) {
            dao.updateRequirement(req.value.id, req.value.choice, req.value.answer)
        }
    }

    ListElementSection(
        "Requirements", requirements,
    ) { req, index ->
        RequirementField(req) {
            scope.launch {
                dao.deleteAnswer(req.value.id)
            }
            deleteIndexFrom(requirements, index)
        }
    }

    Button(
        onClick = {
            // scope.launch {
            //     val id = dao.insertAnswer(choice.value.id, "")
            //     requirements.value = requirements.value.plus(mutableStateOf(Requirement(id, choice.value.id, "")))
            // }
        },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)
    ) {
        Text(text = "+")
    }
}

@Composable
fun RequirementField(
    requirement: MutableState<Requirement>,
    onDelete: () -> Unit,
) {
    Button(
        onClick = {
            // TODO Navigate to edit requirements page
        }
    ) {
        val x = requirement.value
        // TODO
    }
}

@Composable
fun ListAnswerSection(dao: AppDao, choice: MutableState<Choice>) {
    val scope = rememberCoroutineScope()
    val answers = remember { mutableStateOf<List<MutableState<Answer>>>(emptyList()) }
    LaunchedEffect(answers) {
        for(answer in answers.value) {
            dao.updateAnswer(answer.value.choice, answer.value.description)
        }
    }

    ListElementSection(
        "Answers", answers,
    ) { answer, index ->
        UpsertChoiceAnswerField(answer) {
            scope.launch {
                dao.deleteAnswer(answer.value.id)
            }
            deleteIndexFrom(answers, index)
        }
    }

    Button(
        onClick = {
            scope.launch {
                val id = dao.insertAnswer(choice.value.id, "")
                answers.value = answers.value.plus(mutableStateOf(Answer(id, choice.value.id, "")))
            }
        },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)
    ) {
        Text(text = "+")
    }
}

@Composable
private fun <T> ListElementSection(
    itemName: String,
    items: MutableState<List<T>>,
    itemComponent: @Composable (T, Int) -> Unit,
) {
    Text(text = itemName, fontSize = 25.sp)
    LazyColumn {
        items(items.value.withIndex().toList()) { item ->
            itemComponent(item.value, item.index)
        }
    }
}

@Composable
fun UpsertChoiceAnswerField(
    answer: MutableState<Answer>,
    onDelete: () -> Unit
) {
    Row (
        Modifier
            .padding(all = 15.dp)
    ){
        TextField(
            value = answer.value.description,
            onValueChange = { it -> answer.value =  answer.value.copy(description = it)}
        )
        Button(
            onClick = onDelete,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red)
        ) {
            Text(text = "x")
        }
    }
}

@Composable
fun ChoiceForm(dao: AppDao, choice: Choice) {
    val answers = remember { mutableStateOf(emptyList<Answer>()) }
    LaunchedEffect(choice) {
        answers.value = dao.getAnswersFor(choice.id);
    }
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
fun ResultsPage(dao: AppDao) {
    Text(text = "Your decision has been made!", fontSize = 25.sp)
    val decisions = remember { mutableStateOf(emptyList<Result>()) }
    LaunchedEffect(decisions) {
        decisions.value = dao.getResults();
    }
    Column {
        Text("Decisions:", fontSize = 20.sp)
        LazyColumn {
            items(decisions.value) { result ->
                ResultsPage(result)
            }
        }
    }
}

@Composable
fun ResultsPage(result: Result) {
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
