package com.example.decisionbot.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.decisionbot.repository.entity.*

@Database(
    entities = [
        Choice::class, Answer::class,
        Requirement::class, RequirementBox::class,
        ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}