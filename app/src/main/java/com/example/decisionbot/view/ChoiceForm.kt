package com.example.decisionbot.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice

@Composable
@Preview
fun ChoiceForm(
    choice: Choice = Choice("a", "prompt"),
    answers: List<Answer> = listOf(
        Answer("b", "a", "REEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEE"),
        Answer("b", "a", "Option 2"),
        Answer("b", "a", "Option 3")
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(answers) { answer ->
                    Button(onClick = { pick(answer) }) {
                        Text(text = answer.description, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}