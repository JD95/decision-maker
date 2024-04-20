package com.wspcgir.decisionbot

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.wspcgir.decisionbot.destinations.*
import com.wspcgir.decisionbot.repository.AppDatabase
import com.wspcgir.decisionbot.repository.AppRepository
import com.wspcgir.decisionbot.ui.theme.DecisionBotTheme
import com.wspcgir.decisionbot.view.*
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.manualcomposablecalls.composable

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
            MakeEditChoicePage(repo, navArgs, destinationsNavigator)
        }
        composable(ChoiceListPageDestination) {
            ChoiceListPage(viewModel(repo, destinationsNavigator))
        }
        composable(AnswerQuestionPageDestination) {
            MakeAnswerQuestionPage(repo, destinationsNavigator)
        }
        composable(EditRequirementPageDestination) {
            MakeEditRequirementsPage(repo, navArgs, destinationsNavigator)
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

