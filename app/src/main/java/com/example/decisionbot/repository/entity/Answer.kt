package com.example.decisionbot.repository.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Choice::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("id"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Answer(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "choice") val choice: Long,
    @ColumnInfo(name = "description") val description: String,
)
