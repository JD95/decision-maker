package com.wspcgir.decisionbot.view

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wspcgir.decisionbot.model.Result

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
                            .background(
                                color =
                                  if (isSystemInDarkTheme()) Color.DarkGray
                                  else Color.LightGray
                            )
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