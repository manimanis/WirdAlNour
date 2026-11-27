package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SurahPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahPlanDao {
    @Query("SELECT * FROM surah_plan ORDER BY dayNumber ASC, surahNumber ASC")
    fun getAllSurahs(): Flow<List<SurahPlanEntity>>

    @Query("SELECT * FROM surah_plan ORDER BY surahNumber ASC")
    suspend fun getAllSurahsOnce(): List<SurahPlanEntity>

    @Query("SELECT COUNT(*) FROM surah_plan")
    suspend fun getSurahCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surahs: List<SurahPlanEntity>)

    @Query("UPDATE surah_plan SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE surahNumber = :surahNumber")
    suspend fun updateSurahCompletion(surahNumber: Int, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE surah_plan SET isCompleted = :isCompleted, completedAtMillis = :completedAt WHERE dayNumber = :dayNumber")
    suspend fun updateDayCompletion(dayNumber: Int, isCompleted: Boolean, completedAt: Long?)

    @Query("UPDATE surah_plan SET dayNumber = :newDayNumber WHERE surahNumber = :surahNumber")
    suspend fun updateSurahDay(surahNumber: Int, newDayNumber: Int)

    @Query("UPDATE surah_plan SET readingNote = :note WHERE surahNumber = :surahNumber")
    suspend fun updateSurahNote(surahNumber: Int, note: String)

    @Query("UPDATE surah_plan SET isCompleted = 0, completedAtMillis = NULL")
    suspend fun resetAllCompletions()
}
