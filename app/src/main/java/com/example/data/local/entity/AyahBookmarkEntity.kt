package com.example.data.local.entity

import androidx.room.Entity

@Entity(
    tableName = "ayah_bookmarks",
    primaryKeys = ["surahNumber", "ayahNumber"]
)
data class AyahBookmarkEntity(
    val surahNumber: Int,
    val ayahNumber: Int,
    val surahArabicName: String,
    val ayahPreviewText: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
