package com.wspcgir.decisionbot.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wspcgir.decisionbot.destinations.EditChoicePageDestination
import com.wspcgir.decisionbot.destinations.HomePageDestination
import com.wspcgir.decisionbot.repository.AppRepository
import com.wspcgir.decisionbot.repository.entity.Choice
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch

abstract class ChoiceListViewModel : ViewModel() {
    abstract fun getChoices(): List<ListItem<Choice, Unit>>
    abstract fun insertNewChoice(prompt: String)
    abstract fun goHome()
}

private class PreviewChoiceListViewModel: ChoiceListViewModel() {

    override fun getChoices(): List<ListItem<Choice, Unit>> {
        val mut = mutableStateOf(
            listOf(
                Choice("a", "Foo"),
                Choice("b", "Bar"),
                Choice("c", "Apple"),
            )
        )
        return makeListItems(mut, edit = {}, delete = { })
    }

    override fun insertNewChoice(prompt: String) {
    }

    override fun goHome() {
    }

}

@Composable
fun viewModel(repo: AppRepository, nav: DestinationsNavigator): ChoiceListViewModel {
    val choicesMut = remember { mutableStateOf(emptyList<Choice>()) }
    LaunchedEffect(choicesMut) {
        choicesMut.value = repo.getAllChoices().sortedBy { it.prompt }
    }
    return object : ChoiceListViewModel() {

        override fun getChoices(): List<ListItem<Choice, Unit>> {
            return makeListItems(
                choicesMut,
                edit = { x -> nav.navigate(EditChoicePageDestination(x.value)) },
                delete = { viewModelScope.launch { repo.deleteChoice(it) } },
            )
        }

        override fun insertNewChoice(prompt: String) {
            viewModelScope.launch {
                choicesMut.value = listOf(repo.insertChoice(prompt)).plus(choicesMut.value)
            }
        }

        override fun goHome() {
            nav.navigate(HomePageDestination)
        }
    }
}


@Composable
@Destination
fun ChoiceListPage(st: ChoiceListViewModel = PreviewChoiceListViewModel()) {
    Scaffold(floatingActionButton = {
        FloatingActionButton(onClick = { st.goHome() }) {
            Icon(Icons.Default.Home, contentDescription = "go home")
        }
    }) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            ListTitle(title = "Choices") { st.insertNewChoice("New Choice!") }
            EditDeleteBoxList(st.getChoices().sortedBy { it.get().first.prompt }) { requirement ->
                Text(text = requirement.prompt)
            }
        }
    }
}