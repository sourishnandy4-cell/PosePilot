package com.posepilot.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [SessionRecord::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sessionRecordDao(): SessionRecordDao
}
