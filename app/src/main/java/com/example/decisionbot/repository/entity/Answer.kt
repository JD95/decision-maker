package com.example.decisionbot.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Answer(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "choice") val choice: Long,
    @ColumnInfo(name = "description") val description: String,
)
