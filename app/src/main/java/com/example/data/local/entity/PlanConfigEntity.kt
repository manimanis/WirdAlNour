package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plan_config")
data class PlanConfigEntity(
    @PrimaryKey val id: Int = 1,
    val startDateEpochDay: Long,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 20,
    val reminderMinute: Int = 0,
    val completedCycles: Int = 0,
    val themeMode: String = THEME_SYSTEM,
    val readerFontSizeSp: Float = 26f,
    val readerFontKey: String = DEFAULT_READER_FONT
) {
    companion object {
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val DEFAULT_READER_FONT = "amiri_quran"
    }
}
