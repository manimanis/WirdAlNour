package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.SurahAyahEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurahAyahDao {
    @Query("SELECT * FROM surah_ayahs WHERE surahNumber = :surahNumber ORDER BY ayahNumber ASC")
    fun getAyahsForSurah(surahNumber: Int): Flow<List<SurahAyahEntity>>

    @Query("SELECT COUNT(*) FROM surah_ayahs WHERE surahNumber = :surahNumber")
    suspend fun getAyahCountForSurah(surahNumber: Int): Int

    @Query("SELECT COUNT(*) FROM surah_ayahs")
    suspend fun getTotalAyahCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ayahs: List<SurahAyahEntity>)
}
