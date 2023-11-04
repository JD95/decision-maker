package com.example.decisionbot

import androidx.room.Entity

@Entity
data class Result(
    val prompt: String,
    val answer: String
)
