package com.example.decisionbot.repository.entity

import androidx.room.*

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
    ],
    indices = [
        Index(value = ["choice"]),
        Index(value = ["answer"])
    ]
)
@kotlinx.serialization.Serializable
data class Requirement(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "choice") val choice: String,
    @ColumnInfo(name = "answer") val answer: String,
)
