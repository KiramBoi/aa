package com.example.data

import kotlinx.coroutines.flow.Flow

class DreamRepository(private val dreamDao: DreamDao) {
    val allEntries: Flow<List<DreamEntry>> = dreamDao.getAllEntries()

    suspend fun getEntryById(id: Long): DreamEntry? {
        return dreamDao.getEntryById(id)
    }

    suspend fun getEntryByDate(dateString: String): DreamEntry? {
        return dreamDao.getEntryByDate(dateString)
    }

    suspend fun saveEntry(entry: DreamEntry): Long {
        return dreamDao.insertEntry(entry)
    }

    suspend fun deleteEntry(entry: DreamEntry) {
        dreamDao.deleteEntry(entry)
    }

    suspend fun deleteEntryById(id: Long) {
        dreamDao.deleteEntryById(id)
    }
}
