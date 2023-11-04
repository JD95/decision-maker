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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    val scope = rememberCoroutineScope()
    // val navigation = remember { mutableStateOf<Navigation>(Navigation.SelectChoice()) }
    val navController = rememberNavController()

    // val x = navigation.value
    // when (x) {
    //     is Navigation.Home -> { HomePage(dao, navigation) }
    //     is Navigation.SelectChoice -> { SelectChoicePage(dao, navigation) }
    //     is Navigation.EditChoice -> { EditChoicePage(dao, x.choice) }
    //     is Navigation.AnswerQuestion -> { AnswerQuestionPage(dao, navigation) }
    //     is Navigation.SeeResults -> { AnswerQuestionPage(dao, navigation) }
    // }

    NavHost(navController = navController, startDestination = "home") {
        composable("choices") { SelectChoicePage(dao) }

    }
}

@Composable
fun SelectChoicePage(dao: AppDao, navigation: MutableState<Navigation>) {
    val choices = remember { mutableStateOf<List<Choice>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(choices) {
        choices.value = dao.getAllChoices()
    }

    Column {
        Text(text = "Modify Choices", fontSize = 25.sp)

        LazyColumn {
            items(choices.value) { choice ->
                ModifyChoiceCard(choice, navigation)
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
fun HomePage(dao: AppDao, navigation: MutableState<Navigation>) {
    TODO("Not yet implemented")
}

@Composable
fun ModifyChoiceCard(choice: Choice, navigation: MutableState<Navigation>) {
    Button(
        onClick = { navigation.value = Navigation.EditChoice(choice) }
    ) {
        Text(text = choice.prompt)
    }
}

@Composable
fun EditChoicePage(dao: AppDao, initChoice: Choice) {
    val scope = rememberCoroutineScope()
    val choice = remember { mutableStateOf(initChoice) }
    val answers = remember { mutableStateOf<List<MutableState<Answer>>>(emptyList()) }
    val requirements = remember { mutableStateOf<List<MutableState<Requirement>>>(emptyList()) }

    LaunchedEffect(choice) {
        dao.updateChoice(choice.value.id, choice.value.prompt)
    }
    LaunchedEffect(requirements) {
        for(req in requirements.value) {
            dao.updateRequirement(req.value.id, req.value.choice, req.value.answer)
        }
    }

    Column {
        Text(text = "Prompt", fontSize = 25.sp)
        TextField(
            value = choice.value.prompt,
            onValueChange = { it: String -> choice.value = choice.value.copy(prompt = it) }
        )

        ListAnswerSection(dao, choice)

        ListElementSection(
            "Requirements", requirements, scope,
            { requirement, index -> RequirementField(requirement) {
                scope.launch {
                    dao.deleteRequirement(requirement.value.id)
                }
                deleteIndexFrom(requirements, index)
            }},
            {
                mutableStateOf(PartialRequirement.Empty)
            }
        )
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
    navigation: MutableState<Navigation>,
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
        "Answers", requirements,
    ) { req, index ->
        RequirementField(req, navigation) {
            scope.launch {
                dao.deleteAnswer(req.value.id)
            }
            deleteIndexFrom(requirements, index)
        }
    }

    Button(
        onClick = {
            scope.launch {
                val id = dao.insertAnswer(choice.value.id, "")
                requirements.value = requirements.value.plus(mutableStateOf(Requirement(id, choice.value.id, "")))
            }
        },
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Green)
    ) {
        Text(text = "+")
    }
}

@Composable
fun RequirementField(
    requirement: MutableState<Requirement>,
    navigation: MutableState<Navigation>,
    onDelete: () -> Unit,
) {
    Button(
        onClick = {
            navigation.value = Navigation.EditRequirement(requirement)
        }
    ) {
        val x = requirement.value
        when (x) {
            is PartialRequirement.Empty -> Text(text = "New Requirement")
            is PartialRequirement.WithChoice -> Text()
        }
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
