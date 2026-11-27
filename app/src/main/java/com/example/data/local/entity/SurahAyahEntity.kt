package com.example.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "surah_ayahs",
    primaryKeys = ["surahNumber", "ayahNumber"]
)
data class SurahAyahEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val arabicText: String
)
