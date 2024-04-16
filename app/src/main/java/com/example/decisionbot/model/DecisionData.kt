package com.example.decisionbot.model

import com.example.decisionbot.repository.entity.Answer
import com.example.decisionbot.repository.entity.Choice
import com.example.decisionbot.repository.entity.Requirement

data class DecisionData(
    val choice: Choice,
    val answers: List<Answer>,
    val requirements: List<Requirement>
)