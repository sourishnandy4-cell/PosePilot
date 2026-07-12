package com.posepilot.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionRecordDao {
    @Query("SELECT * FROM session_records ORDER BY timestampMs DESC")
    fun getAllRecords(): Flow<List<SessionRecord>>

    @Insert
    suspend fun insertRecord(record: SessionRecord)

    @Query("DELETE FROM session_records")
    suspend fun clearAllRecords()
}
