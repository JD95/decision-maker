package com.wspcgir.decisionbot.repository

import androidx.room.Database
import androidx.room.RoomDatabase
import com.wspcgir.decisionbot.repository.entity.*

@Database(
    entities = [
        Choice::class, Answer::class,
        Requirement::class, RequirementBox::class,
        ],
    version = 1,
    autoMigrations = [ ],
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao
}