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
            childColumns = arrayOf("choice"),
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Answer::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("answer"),
            onDelete = ForeignKey.CASCADE
        ),
    ]
)
@kotlinx.serialization.Serializable
data class Requirement(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "choice") val choice: Long,
    @ColumnInfo(name = "answer") val answer: Long,
)
