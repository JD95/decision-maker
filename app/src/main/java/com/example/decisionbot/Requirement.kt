package com.example.decisionbot

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Requirement(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "choice") val choice: Long,
    @ColumnInfo(name = "answer") val answer: Long,
)
