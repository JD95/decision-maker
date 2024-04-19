@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalComposeUiApi::class,
    ExperimentalComposeUiApi::class
)

package com.example.decisionbot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.decisionbot.destinations.*
import com.example.decisionbot.model.DecisionData
import com.example.decisionbot.model.Result
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
        Log.i("startup", "Starting up app!")
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
                    MainComponent(AppRepository(dao))
                }
            }
        }
    }
}


data class AnswerFieldContext(
    val inEdit: MutableState<Boolean>,
    val state: MutableState<Answer>
)

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

                override fun getAnswers(): List<ListItem<Answer, AnswerFieldContext>> {
                    viewModelScope.launch {
                        answersMut.value = repo.getAnswersForChoice(choice.value)
                        val msg = answersMut.value
                            .map { "${it.id}: '${it.description}', " }
                            .fold("") { x, y -> x.plus(y) }
                        Log.d("getAnswers", "Answers for choice:${choice.value.id} = $msg")
                    }
                    return makeListItems(
                        answersMut,
                        editContext = {
                            AnswerFieldContext(
                                inEdit = mutableStateOf(false),
                                state = it
                            )
                        },
                        edit = { _, ctx -> ctx.inEdit.value = true },
                        { item -> viewModelScope.launch { repo.deleteAnswer(item) } }
                    )
                }

                override fun updateAnswer(value: MutableState<Answer>, newDescription: String) {
                    val newAnswer = value.value.copy(description = newDescription)
                    viewModelScope.launch {
                        repo.editAnswer(newAnswer)
                    }
                    value.value = newAnswer
                }

                override fun getRequirements(): List<ListItem<RequirementBox, Unit>> {
                    viewModelScope.launch {
                        requirementsMut.value = repo.getRequirementBoxInfoFor(choice.value)
                    }
                    return makeListItems(
                        requirementsMut,
                        edit = { item ->
                            destinationsNavigator.navigate(
                                EditRequirementPageDestination(
                                    choice.value,
                                    Requirement(item.value.id, item.value.choice, item.value.answer)
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

                override fun gotoChoiceListPage() {
                    destinationsNavigator.navigate(ChoiceListPageDestination)
                }
            })
        }
        composable(ChoiceListPageDestination) {
            ChoiceListPage(makeChoiceListViewModel(repo, destinationsNavigator))
        }
        composable(AnswerQuestionPageDestination) {
            val current = remember { mutableStateOf<DecisionData?>(null) }
            val resultsMut = remember { mutableStateOf(emptyList<Result>()) }
            val choices = remember { mutableStateOf(emptyList<DecisionData>()) }
            val decisions = remember { mutableStateOf(emptyList<Answer>()) }

            LaunchedEffect(Unit) {
                choices.value = repo.getAllDecisionData()
                setupNextQuestionForDecision(choices, decisions.value, current)
            }

            AnswerQuestionPage(object : AnswerQuestionPageViewModel() {
                override val current: State<DecisionData?>
                    get() = current

                override fun pick(value: Answer) {
                    Log.d("pick", "Answer '${value.description}' picked")
                    viewModelScope.launch {
                        decisions.value = decisions.value.plus(value)
                        Log.d("pick", "current decisions: ${decisions.value}")
                        val answered = current.value
                        if (answered != null) {
                            choices.value = choices.value.minus(answered)
                        }
                        Log.d("pick", "current choices: ${choices.value}")
                        setupNextQuestionForDecision(
                            choices,
                            decisions.value,
                            current
                        )
                    }
                }

                override fun getResults(): State<List<Result>> {
                    viewModelScope.launch {
                        resultsMut.value = decisions.value.map {
                            val choice = repo.getChoiceForAnswer(it)
                            Result(choice.prompt, it.description)
                        }
                    }
                    return resultsMut
                }

                override fun resetDecisions() {
                    destinationsNavigator.navigate(HomePageDestination)
                }

            })
        }
        composable(EditRequirementPageDestination) {
            val scope = rememberCoroutineScope()
            val args = this.navArgs
            val parentChoice = navArgs.parentChoice
            val requirement = args.requirement
            val answerIndex = remember { mutableStateOf(0) }
            val choiceIndex = remember { mutableStateOf(0) }
            val choices = remember { mutableStateOf<List<Choice>>(emptyList()) }
            val choice = remember { mutableStateOf<Choice?>(null) }
            val answers = remember { mutableStateOf<List<Answer>>(emptyList()) }
            val answer = remember { mutableStateOf<Answer?>(null) }

            LaunchedEffect(scope) {
                choices.value = repo.getAllChoices().filter { it.id != args.parentChoice.id }
                val givenChoice = if (args.requirement != null) {
                    repo.getChoiceForRequirement(args.requirement)
                } else {
                    choices.value.firstOrNull()
                }

                Log.d("edit-requirement", "Choice is '$givenChoice'")

                choice.value = givenChoice

                val givenAnswers = if (givenChoice != null) {
                    repo.getAnswersForChoice(givenChoice)
                } else {
                    emptyList()
                }

                answers.value = givenAnswers

                val givenAnswer = if (args.requirement != null) {
                    repo.getAnswerForRequirement(args.requirement)
                } else {
                    givenAnswers.firstOrNull()
                }

                Log.d("edit-requirement", "Answer is '$givenAnswer'")

                answer.value = givenAnswer

                if (givenChoice != null) {
                    choiceIndex.value = choices.value.indexOfFirst {
                        it.id == givenChoice.id
                    }
                }

                Log.d("edit-requirement", "Choice index is '${choiceIndex.value}'")

                if (givenAnswer != null) {
                    answerIndex.value = answers.value.indexOfFirst {
                        it.id == givenAnswer.id
                    }
                }

                Log.d("edit-requirement", "Answer index is '${answerIndex.value}'")
            }

            EditRequirementPage(object : EditRequirementsPageViewModel() {

                override val selectedChoiceIndex: State<Int>
                    get() = choiceIndex
                override val selectedAnswerIndex: State<Int>
                    get() = answerIndex

                override fun allChoices(): List<Choice> {
                    return choices.value
                }

                override fun answersForChoice(): List<Answer> {
                    return answers.value
                }

                override fun chooseChoice(index: Int, item: Choice) {
                    choice.value = item
                    choiceIndex.value = index
                    viewModelScope.launch {
                        answers.value = repo.getAnswersForChoice(item)
                        answer.value = answers.value.firstOrNull()
                        answerIndex.value = 0
                    }
                }

                override fun chooseAnswer(index: Int, item: Answer) {
                    answer.value = item
                    answerIndex.value = index
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

fun setupNextQuestionForDecision(
    choicesSt: MutableState<List<DecisionData>>,
    decisions: List<Answer>,
    current: MutableState<DecisionData?>,
) {
    choicesSt.value = choicesSt.value.map {
        val unsatisfied = it.requirements.filter { r ->
            decisions.none { a -> a.id == r.answer }
        }
        it.copy(requirements = unsatisfied)
    }

    Log.d("pick", "choices after filter: ${choicesSt.value}")

    current.value = choicesSt.value.find { it.requirements.isEmpty() }
}


fun makeChoiceListViewModel(repo: AppRepository, nav: DestinationsNavigator): ChoiceListViewModel {
    return object : ChoiceListViewModel() {
        override fun getChoices(): List<ListItem<Choice, Unit>> {
            viewModelScope.launch {
                choicesMut.value = repo.getAllChoices()
            }
            return makeListItems(
                choicesMut,
                edit = { x -> nav.navigate(EditChoicePageDestination(x.value)) },
                delete = { viewModelScope.launch { repo.deleteChoice(it) } },
            )
        }

        override fun insertNewChoice(prompt: String) {
            viewModelScope.launch {
                choicesMut.value = choices.value.plus(repo.insertChoice(prompt))
            }
        }

        override fun goHome() {
            nav.navigate(HomePageDestination)
        }
    }
}

@Composable
@Preview(showBackground = true)
@Destination(start = true)
fun HomePage(
    gotoChoiceListPage: () -> Unit = { },
    gotoDecisionPage: () -> Unit = { }
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Decision", fontSize = 60.sp)
        Text("Bot", fontSize = 60.sp)
        Spacer(modifier = Modifier.fillMaxSize(0.5f))
        Button(onClick = { gotoChoiceListPage() }) {
            Text(text = "Edit Choices", fontSize = 30.sp)
        }
        Spacer(modifier = Modifier.fillMaxSize(0.1f))
        Button(onClick = { gotoDecisionPage() }) {
            Text(text = "Make Decision", fontSize = 30.sp)
        }
    }
}

abstract class ChoiceListViewModel : ViewModel() {
    protected val choicesMut = mutableStateOf(emptyList<Choice>())
    val choices: State<List<Choice>> = choicesMut
    abstract fun getChoices(): List<ListItem<Choice, Unit>>
    abstract fun insertNewChoice(prompt: String)
    abstract fun goHome()
}

@Composable
@Destination
fun ChoiceListPage(st: ChoiceListViewModel) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { st.goHome() }) {
            Icon(Icons.Default.Home, contentDescription = "go home")
        }
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListTitle(title = "Choices") { st.insertNewChoice("New Choice!") }
            EditDeleteBoxList(st.getChoices()) { requirement ->
                Text(text = requirement.prompt)
            }
        }
    }
}

abstract class AnswerQuestionPageViewModel : ViewModel() {

    abstract val current: State<DecisionData?>
    abstract fun pick(value: Answer)
    abstract fun getResults(): State<List<Result>>

    abstract fun resetDecisions()
}

@Composable
@Destination
fun AnswerQuestionPage(st: AnswerQuestionPageViewModel) {
    Column {
        val current = st.current.value
        if (current != null) {
            ChoiceForm(
                choice = current.choice,
                answers = current.answers,
                pick = { st.pick(it) },
                random = {
                    val index = (0 until current.answers.size).random()
                    st.pick(current.answers[index])
                }
            )
        } else {
            ResultsList(st.getResults().value) { st.resetDecisions() }
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

    abstract fun getAnswers(): List<ListItem<Answer, AnswerFieldContext>>

    abstract fun updateAnswer(value: MutableState<Answer>, newDescription: String)

    abstract fun getRequirements(): List<ListItem<RequirementBox, Unit>>
    abstract fun newAnswer()

    abstract fun newRequirement()

    abstract fun gotoChoiceListPage()
}

fun <T> makeListItems(
    items: MutableState<List<T>>,
    edit: (MutableState<T>) -> Unit,
    delete: (T) -> Unit
): List<ListItem<T, Unit>> {
    return makeListItems(
        items,
        editContext = { },
        edit = { x, _ -> edit(x) },
        delete = delete
    )
}

fun <T, K> makeListItems(
    items: MutableState<List<T>>,
    editContext: (MutableState<T>) -> K,
    edit: (MutableState<T>, K) -> Unit,
    delete: (T) -> Unit
): List<ListItem<T, K>> {
    return items.value.map { t ->
        val st = mutableStateOf(t)
        val ctx = editContext(st)
        ListItem(
            get = { Pair(st.value, ctx) },
            edit = { edit(st, ctx) },
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

@OptIn(ExperimentalComposeUiApi::class)
@Destination(navArgsDelegate = EditChoicePageNavArgs::class)
@Composable
fun EditChoicePage(
    st: EditChoicePageViewModel
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    st.saveChoice()
                    st.gotoChoiceListPage()
                }
            ) {
                Icon(Icons.Default.Done, contentDescription = "save choice")
            }
        }) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding)) {
            OutlinedTextField(
                label = { Text(text = "Prompt", fontSize = 20.sp) },
                value = st.choice.value.prompt,
                onValueChange = { st.updateChoicePrompt(it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    st.saveChoice()
                    keyboardController?.hide()
                })
            )

            ListTitle("Answers") { st.newAnswer() }
            EditDeleteBoxList(st.getAnswers()) { answer, ctx ->
                if (ctx.inEdit.value) {
                    val text = remember { mutableStateOf(answer.description) }
                    TextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                st.updateAnswer(ctx.state, text.value)
                                ctx.inEdit.value = false
                                keyboardController?.hide()
                            }
                        )
                    )
                } else {
                    Text(text = answer.description)
                }
            }

            ListTitle("Requirements") { st.newRequirement() }
            EditDeleteBoxList(st.getRequirements()) { requirement ->
                Text(text = requirement.prompt)
                Spacer(Modifier.width(10.dp))
                Text(text = requirement.description)
            }
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

data class ListItem<T, K>(
    val edit: () -> Unit,
    val delete: () -> Unit,
    val get: () -> Pair<T, K>
)

@Composable
fun <Item> EditDeleteBoxList(
    items: List<ListItem<Item, Unit>>,
    displayItem: @Composable (Item) -> Unit,
) {
    EditDeleteBoxList(items) { x, _ -> displayItem(x) }
}

@Composable
fun <Item, Context> EditDeleteBoxList(
    items: List<ListItem<Item, Context>>,
    displayItem: @Composable (Item, Context) -> Unit,
) {
    LazyColumn {
        items(items) { item ->
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val pair = item.get()
                    displayItem(pair.first, pair.second)
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

abstract class EditRequirementsPageViewModel : ViewModel() {

    abstract val selectedChoiceIndex: State<Int>
    abstract val selectedAnswerIndex: State<Int>

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
                selectedItemToString = { it.prompt },
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
                selectedItemToString = { it.description },
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
@Preview
fun ChoiceForm(
    choice: Choice = Choice("a", "Some prompt"),
    answers: List<Answer> = listOf(
        Answer("b", "a", "Option 1"),
        Answer("b", "a", "Option 2")
    ),
    pick: (Answer) -> Unit = { _ -> },
    random: () -> Unit = { }
) {
    Scaffold(floatingActionButton = {
        Button(onClick = { random() }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ArrowForward, contentDescription = "choose randomly")
                Text("chaos")
            }
        }
    }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = choice.prompt, fontSize = 25.sp)
            Spacer(modifier = Modifier.fillMaxSize(0.25f))
            LazyColumn {
                items(answers) { answer ->
                    Button(onClick = { pick(answer) }) {
                        Text(text = answer.description, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
@Preview
fun ResultsList(
    decisions: List<Result> = listOf(
        Result("Stay in or Go out?", "Stay In"),
        Result("What are we eating?", "Tacos"),
        Result("Who's place?", "Mine"),
        Result("Where are we getting the food?", "Outside"),
        Result("What movie are we watching?", "Inception"),
        Result("Are we playing board games?", "Yes"),
        Result("Which game?", "Catan"),
    ),
    goHome: () -> Unit = { }
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { goHome() }) {
                Icon(Icons.Default.Home, contentDescription = "go home")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.fillMaxSize(0.1f))
            Text("Decisions", fontSize = 50.sp)
            Spacer(Modifier.fillMaxSize(0.1f))
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(decisions) { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .background(color = Color.LightGray)
                            .padding(vertical = 5.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = result.prompt,
                            textAlign = TextAlign.Center,
                            fontSize = 25.sp,
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = result.answer,
                            textAlign = TextAlign.Center,
                            fontSize = 25.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}