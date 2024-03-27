package com.example.decisionbot.repository.entity

import androidx.room.Entity

@Entity
data class Result(
    val prompt: String,
    val answer: String
)
