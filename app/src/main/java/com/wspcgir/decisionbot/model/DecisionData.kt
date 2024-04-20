package com.wspcgir.decisionbot.model

import com.wspcgir.decisionbot.repository.entity.Answer
import com.wspcgir.decisionbot.repository.entity.Choice
import com.wspcgir.decisionbot.repository.entity.Requirement

data class DecisionData(
    val choice: Choice,
    val answers: List<Answer>,
    val requirements: List<Requirement>
)