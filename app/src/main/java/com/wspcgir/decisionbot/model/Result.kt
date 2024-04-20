package com.wspcgir.decisionbot.model

import androidx.room.Entity

@Entity
data class Result(
    val prompt: String,
    val answer: String
)
