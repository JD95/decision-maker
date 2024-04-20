package com.example.decisionbot.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
    val choiceMut = remember { mutableStateOf(navArgs.choice) }
    val answersMut = remember { mutableStateOf(emptyList<Answer>()) }
    val requirementsMut = remember { mutableStateOf(emptyList<RequirementBox>()) }
    LaunchedEffect(choiceMut) {
        requirementsMut.value = repo.getRequirementBoxInfoFor(choiceMut.value)
    }

    LaunchedEffect(choiceMut) {
        answersMut.value = repo.getAnswersForChoice(choiceMut.value)
        val msg = answersMut.value
            .map { "${it.id}: '${it.description}', " }
            .fold("") { x, y -> x.plus(y) }
        Log.d("getAnswers", "Answers for choice:${choiceMut.value.id} = $msg")
    }

    EditChoicePage(object : EditChoicePageViewModel() {
        override val choice: State<Choice>
            get() = choiceMut

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

abstract class EditChoicePageViewModel : ViewModel() {
    abstract val choice: State<Choice>

    abstract fun updateChoicePrompt(prompt: String)

    abstract fun saveChoice()

    abstract fun getAnswers(): List<ListItem<Answer, AnswerFieldContext>>

    abstract fun updateAnswer(value: MutableState<Answer>, newDescription: String)

    abstract fun getRequirements(): List<ListItem<RequirementBox, Unit>>
    abstract fun newAnswer()

    abstract fun newRequirement()

    abstract fun gotoChoiceListPage()
}

private class EditChoicePagePreview : EditChoicePageViewModel() {
    override val choice: State<Choice>
        get() = mutableStateOf(Choice("a", "What's up?"))

    override fun updateChoicePrompt(prompt: String) {
    }

    override fun saveChoice() {
    }

    override fun getAnswers(): List<ListItem<Answer, AnswerFieldContext>> {
        return listOf(
            ListItem(
                edit = { },
                delete = { },
                get = {
                    val answer = Answer("bleh", "a", "sky")
                    Pair(
                        answer,
                        AnswerFieldContext(
                            mutableStateOf(false),
                            mutableStateOf(answer)
                        )
                    )
                })
        )
    }

    override fun updateAnswer(value: MutableState<Answer>, newDescription: String) {
    }

    override fun getRequirements(): List<ListItem<RequirementBox, Unit>> {
        return listOf(
            ListItem(
                edit = { },
                delete = { },
                get = {
                    val req = RequirementBox(
                        "bleh",
                        "a",
                        "b",
                        "Sup?",
                        "YoMama"
                    )
                    Pair(req, Unit)
                })
        )
    }

    override fun newAnswer() {
    }

    override fun newRequirement() {
    }

    override fun gotoChoiceListPage() {
    }

}

data class EditChoicePageNavArgs(
    val choice: Choice
)

@OptIn(ExperimentalComposeUiApi::class)
@Destination(navArgsDelegate = EditChoicePageNavArgs::class)
@Composable
@Preview
fun EditChoicePage(
    st: EditChoicePageViewModel = EditChoicePagePreview()
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
                    Text(text = answer.description, modifier = Modifier.weight(1f, fill = false))
                }
            }

            ListTitle("Requirements") { st.newRequirement() }
            EditDeleteBoxList(st.getRequirements()) { requirement ->
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = requirement.prompt, textAlign = TextAlign.Center)
                    Text(text = requirement.description, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

data class AnswerFieldContext(
    val inEdit: MutableState<Boolean>,
    val state: MutableState<Answer>
)