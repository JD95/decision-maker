package com.example.decisionbot.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decisionbot.destinations.EditChoicePageDestination
import com.example.decisionbot.repository.AppRepository
import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice
import com.example.decisionbot.repository.entity.Requirement
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Composable
fun MakeEditRequirementsPage(
    repo: AppRepository,
    args: EditRequirementPageNavArgs,
    destinationsNavigator: DestinationsNavigator
) {
    val scope = rememberCoroutineScope()
    val parentChoice = args.parentChoice
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