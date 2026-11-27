package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AyahBookmarkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AyahBookmarkDao {
    @Query("SELECT * FROM ayah_bookmarks ORDER BY createdAtMillis DESC")
    fun getAllBookmarks(): Flow<List<AyahBookmarkEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM ayah_bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber)")
    suspend fun isBookmarked(surahNumber: Int, ayahNumber: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBookmark(bookmark: AyahBookmarkEntity)

    @Query("DELETE FROM ayah_bookmarks WHERE surahNumber = :surahNumber AND ayahNumber = :ayahNumber")
    suspend fun deleteBookmark(surahNumber: Int, ayahNumber: Int)
}
