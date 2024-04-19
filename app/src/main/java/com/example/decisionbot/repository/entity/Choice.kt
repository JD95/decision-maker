package com.example.decisionbot.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
@kotlinx.serialization.Serializable
data class Choice (
    @PrimaryKey val id: String,
    @ColumnInfo(name = "prompt") val prompt: String,
)
