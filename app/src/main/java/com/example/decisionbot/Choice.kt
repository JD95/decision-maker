package com.example.decisionbot

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Choice (
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "prompt") val prompt: String,
)
