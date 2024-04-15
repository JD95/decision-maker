package com.example.decisionbot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.decisionbot.destinations.*
import com.example.decisionbot.repository.AppDao
import com.example.decisionbot.repository.AppDatabase
import com.example.decisionbot.repository.AppRepository
import com.example.decisionbot.repository.entity.*
import com.example.decisionbot.ui.theme.DecisionBotTheme
import com.example.decisionbot.view.LargeDropDownMenu
import com.example.decisionbot.view.LargeDropDownMenuItem
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
    dao.insertRequirement(movieOrAnime, stayIn)
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
            EditChoicePage(object : EditChoicePageViewModel(navArgs.choice) {

                override fun updateChoicePrompt(prompt: String) {
                    val updatedChoice = choice.value.copy(prompt = prompt)
                    choiceMut.value = updatedChoice
                }

                override fun saveChoice() {
                    viewModelScope.launch {
                        repo.editChoice(choiceMut.value)
                    }
                }

                override fun getAnswers(): List<ListItem<Answer>> {
                    viewModelScope.launch {
                        answersMut.value = repo.getAnswersForChoice(choice.value)
                    }
                    return makeListItems(
                        answersMut,
                        { item -> viewModelScope.launch { repo.editAnswer(item) } },
                        { item -> viewModelScope.launch { repo.deleteAnswer(item) } }
                    )
                }

                override fun getRequirements(): List<ListItem<RequirementBox>> {
                    viewModelScope.launch {
                        requirementsMut.value = repo.getRequirementBoxInfoFor(choice.value)
                    }
                    return makeListItems(
                        requirementsMut,
                        { item ->
                            destinationsNavigator.navigate(
                                EditRequirementPageDestination(
                                    choice.value,
                                    Requirement(item.id, item.choice, item.answer)
                                )
                            )
                        },
                        { item -> viewModelScope.launch { repo.deleteRequirementBox(item) } }
                    )
                }

                override fun newAnswer() {
                    viewModelScope.launch {
                        val new = repo.insertAnswer(choice.value, "new answer")
                        answersMut.value = answersMut.value.plus(new)
                    }
                }

                override fun newRequirement() {
                    destinationsNavigator.navigate(
                        EditRequirementPageDestination(choice.value, null)
                    )
                }
            })
        }
        composable(ChoiceListPageDestination) {
            ChoiceListPage(makeChoiceListViewModel(repo, destinationsNavigator))
        }
        composable(AnswerQuestionPageDestination) {
            AnswerQuestionPage(object : AnswerQuestionPageViewModel() {
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
        composable(EditRequirementPageDestination) {
            val scope = rememberCoroutineScope()
            val args = this.navArgs
            val answerIndex = remember { mutableStateOf(0) }
            val choiceIndex = remember { mutableStateOf(0) }
            val choices = remember { mutableStateOf<List<Choice>>(emptyList()) }
            val choice = remember { mutableStateOf<Choice?>(null) }
            val answers = remember { mutableStateOf<List<Answer>>(emptyList()) }
            val answer = remember { mutableStateOf<Answer?>(null) }

            LaunchedEffect(scope) {
                choices.value = repo.getAllChoices()
                if (args.requirement != null) {
                    val givenChoice = repo.getChoiceForRequirement(args.requirement)
                    choiceIndex.value = choices.value.indexOfFirst {
                        it.id == givenChoice.id
                    }
                    answers.value = repo.getAnswersForChoice(givenChoice)
                    answerIndex.value = answers.value.indexOfFirst {
                        it.id == args.requirement.answer
                    }
                } else {
                    if (choices.value.isNotEmpty()) {
                        answers.value = repo.getAnswersForChoice(
                            choices.value[choiceIndex.value]
                        )
                    }
                }
            }

            EditRequirementPage(object : EditRequirementsPageViewModel(
                parentChoice = navArgs.parentChoice,
                requirement = args.requirement,
                choices = choices,
                choice = choice,
                selectedAnswerIndexMut = answerIndex,
                selectedChoiceIndexMut = choiceIndex,
                answer = answer,
                answersMut = answers
            ) {

                override fun allChoices(): List<Choice> {
                    return choices.value
                }

                override fun answersForChoice(): List<Answer> {
                    val theChoice = choice.value
                    viewModelScope.launch {
                        if (theChoice != null) {
                            answersMut.value = repo.getAnswersForChoice(theChoice)
                        }
                    }
                    return answersMut.value
                }

                override fun chooseChoice(index: Int, item: Choice) {
                    choice.value = item
                    selectedChoiceIndexMut.value = index
                }

                override fun chooseAnswer(index: Int, item: Answer) {
                    answer.value = item
                    selectedAnswerIndexMut.value = index
                }

                override fun close() {
                    viewModelScope.launch {
                        val chosenAnswer = answer.value
                        if (chosenAnswer != null) {
                            if (requirement != null) {
                                repo.editRequirement(
                                    requirement.copy(
                                        choice = parentChoice.id,
                                        answer = chosenAnswer.id
                                    )
                                )
                            } else {
                                repo.insertRequirement(parentChoice, chosenAnswer)
                            }
                        }
                    }
                    destinationsNavigator.navigate(EditChoicePageDestination(parentChoice))
                }
            })
        }
    }
}

fun makeChoiceListViewModel(repo: AppRepository, nav: DestinationsNavigator): ChoiceListViewModel {
    return object : ChoiceListViewModel() {
        override fun getChoices(): List<ListItem<Choice>> {
            viewModelScope.launch {
                choicesMut.value = repo.getAllChoices()
            }
            return makeListItems(
                choicesMut,
                edit = { nav.navigate(EditChoicePageDestination(it)) },
                delete = { viewModelScope.launch {  repo.deleteChoice(it) } },
            )
        }

        override fun insertNewChoice(prompt: String) {
            viewModelScope.launch {
                choicesMut.value = choices.value.plus(repo.insertChoice(prompt))
            }
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

abstract class ChoiceListViewModel : ViewModel() {
    protected val choicesMut = mutableStateOf(emptyList<Choice>())
    val choices: State<List<Choice>> = choicesMut
    abstract fun getChoices(): List<ListItem<Choice>>
    abstract fun insertNewChoice(prompt: String)
}

@Composable
@Destination
fun ChoiceListPage(st: ChoiceListViewModel) {
    Column {
        ListTitle(title = "Choices") { st.insertNewChoice("New Choice!") }
        EditDeleteBoxList(st.getChoices()) { requirement ->
            Text(text = requirement.prompt)
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

    abstract fun saveChoice()

    abstract fun getAnswers(): List<ListItem<Answer>>
    abstract fun getRequirements(): List<ListItem<RequirementBox>>
    abstract fun newAnswer()

    abstract fun newRequirement()
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
            edit = { edit(t) },
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
            modifier = Modifier.onFocusChanged { focus ->
                if (!focus.isFocused) {
                    st.saveChoice()
                }
            },
            value = st.choice.value.prompt,
            onValueChange = { st.updateChoicePrompt(it) }
        )

        ListTitle("Answers") { st.newAnswer() }
        EditDeleteBoxList(st.getAnswers()) { answer ->
            Text(text = answer.description)
        }

        ListTitle("Requirements") { st.newRequirement() }
        EditDeleteBoxList(st.getRequirements()) { requirement ->
            Text(text = requirement.prompt)
            Text(text = requirement.description)
        }
    }
}

@Composable
fun ListTitle(title: String, newItem: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, fontSize = 25.sp)
        TextButton(onClick = newItem) {
            Icon(Icons.Default.Add, contentDescription = "add")
        }
    }
}

data class ListItem<T>(
    val edit: () -> Unit,
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
                    TextButton(onClick = { item.edit() }) {
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

abstract class EditRequirementsPageViewModel(
    val parentChoice: Choice,
    val requirement: Requirement?,
    val choices: MutableState<List<Choice>>,
    val choice: MutableState<Choice?>,
    val answer: MutableState<Answer?>,
    val answersMut: MutableState<List<Answer>>,
    val selectedChoiceIndexMut: MutableState<Int>,
    val selectedAnswerIndexMut: MutableState<Int>,
) : ViewModel() {

    val selectedChoiceIndex: State<Int> = selectedChoiceIndexMut
    val selectedAnswerIndex: State<Int> = selectedAnswerIndexMut

    abstract fun allChoices(): List<Choice>
    abstract fun answersForChoice(): List<Answer>
    abstract fun chooseChoice(index: Int, item: Choice)

    abstract fun chooseAnswer(index: Int, item: Answer)

    abstract fun close()
}

data class EditRequirementPageNavArgs(
    val parentChoice: Choice,
    val requirement: Requirement?
)

@Composable
@Destination(navArgsDelegate = EditRequirementPageNavArgs::class)
fun EditRequirementPage(st: EditRequirementsPageViewModel) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { st.close() }) {
            Icon(Icons.Default.Done, contentDescription = "save")
        }
    }) { innerPadding ->
        remember { st.selectedChoiceIndex }
        remember { st.selectedAnswerIndex }
        Column(modifier = Modifier.padding(innerPadding)) {
            Text(text = "Edit Requirement", fontSize = 30.sp)
            Spacer(modifier = Modifier.height(10.dp))
            LargeDropDownMenu(
                label = "Choice",
                items = st.allChoices(),
                selectedIndex = st.selectedChoiceIndex.value,
                onItemSelected = st::chooseChoice,
                selectedItemToString = { it -> it.prompt },
                drawItem = { item, selected, enabled, onClick ->
                    LargeDropDownMenuItem(
                        text = item.prompt,
                        selected = selected,
                        enabled = enabled,
                        onClick = onClick
                    )
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            LargeDropDownMenu(
                label = "Answer",
                items = st.answersForChoice(),
                selectedIndex = st.selectedAnswerIndex.value,
                onItemSelected = st::chooseAnswer,
                selectedItemToString = { it -> it.description },
                drawItem = { item, selected, enabled, onClick ->
                    LargeDropDownMenuItem(
                        text = item.description,
                        selected = selected,
                        enabled = enabled,
                        onClick = onClick
                    )
                }
            )
        }
    }
}

@Composable
fun ChoiceForm(choice: Choice, answers: List<Answer>) {
    Text(text = choice.prompt, fontSize = 25.sp)
    Column {
        LazyColumn {
            items(answers) { answer ->
                AnswerField(answer)
            }
        }
    }
}

@Composable
fun AnswerField(answer: Answer) {
    Button(onClick = { }) {
        Text(text = answer.description, fontSize = 20.sp)
    }
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
