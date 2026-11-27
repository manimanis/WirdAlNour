package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "surah_plan")
data class SurahPlanEntity(
    @PrimaryKey val surahNumber: Int,
    val arabicName: String,
    val ayahCount: Int,
    val revelationType: String,
    val startPage: Int,
    val juzNumber: Int,
    val dayNumber: Int,
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val readingNote: String = "",
    val themeSummary: String = ""
)
