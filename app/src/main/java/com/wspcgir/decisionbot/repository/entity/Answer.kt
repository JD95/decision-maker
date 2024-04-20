package com.wspcgir.decisionbot.repository.entity

import androidx.room.*

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Choice::class,
            parentColumns = arrayOf("id"),
            childColumns = arrayOf("choice"),
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["choice"])
    ]
)
data class Answer(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "choice") val choice: String,
    @ColumnInfo(name = "description") val description: String,
)
