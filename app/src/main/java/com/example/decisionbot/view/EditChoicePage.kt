package com.example.decisionbot.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decisionbot.destinations.ChoiceListPageDestination
import com.example.decisionbot.destinations.EditRequirementPageDestination
import com.example.decisionbot.repository.AppRepository
import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice
import com.example.decisionbot.repository.entity.Requirement
import com.example.decisionbot.repository.entity.RequirementBox
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Composable
fun MakeEditChoicePage(
    repo: AppRepository,
    navArgs: EditChoicePageNavArgs,
    destinationsNavigator: DestinationsNavigator
) {
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

data class AnswerFieldContext(
    val inEdit: MutableState<Boolean>,
    val state: MutableState<Answer>
)