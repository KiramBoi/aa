package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamDao {
    @Query("SELECT * FROM dream_entries ORDER BY dateString DESC, timestamp DESC")
    fun getAllEntries(): Flow<List<DreamEntry>>

    @Query("SELECT * FROM dream_entries WHERE id = :id LIMIT 1")
    suspend fun getEntryById(id: Long): DreamEntry?

    @Query("SELECT * FROM dream_entries WHERE dateString = :dateString LIMIT 1")
    suspend fun getEntryByDate(dateString: String): DreamEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: DreamEntry): Long

    @Delete
    suspend fun deleteEntry(entry: DreamEntry)

    @Query("DELETE FROM dream_entries WHERE id = :id")
    suspend fun deleteEntryById(id: Long)
}
