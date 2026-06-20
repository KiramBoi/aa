package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "dream_entries")
data class DreamEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateString: String, // format: "yyyy-MM-dd"
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    
    // Lucid Dream checklist & options
    val isLucid: Boolean = false,
    val lucidIntensity: Int = 5, // 1 to 10
    val lucidClarity: Int = 5, // 1 to 10
    val lucidDescription: String = "",
    val lucidAudioPath: String? = null,
    
    // Dream Recall checklist & options
    val hasDreamRecall: Boolean = false,
    val recallLevel: Int = 5, // 1 to 10
    val recallDescription: String = "",
    val recallAudioPath: String? = null,
    
    // Sleep rating / general metrics
    val sleepRating: Int = 3, // 1 to 5 stars
    val tags: String = "", // comma-separated strings (e.g. "flying, ocean, stars")
    val isFavorite: Boolean = false
) : Serializable
