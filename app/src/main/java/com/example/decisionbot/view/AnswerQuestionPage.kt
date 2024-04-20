package com.example.decisionbot.view

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.decisionbot.destinations.HomePageDestination
import com.example.decisionbot.model.DecisionData
import com.example.decisionbot.model.Result
import com.example.decisionbot.repository.AppRepository
import com.example.decisionbot.repository.entity.Answer
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

@Composable
fun MakeAnswerQuestionPage(
    repo: AppRepository,
    destinationsNavigator: DestinationsNavigator
) {
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