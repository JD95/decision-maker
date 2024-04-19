package com.example.decisionbot.repository.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RequirementBox(
    @PrimaryKey
    val id: String,
    val choice: String,
    val answer: String,
    val prompt: String,
    val description: String
)