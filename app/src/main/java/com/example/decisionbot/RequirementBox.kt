package com.example.decisionbot

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RequirementBox(
    @PrimaryKey
    val id: Long,
    val choice: Long,
    val answer: Long,
    val prompt: String,
    val description: String
)