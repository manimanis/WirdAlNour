package com.example.data.repository

import com.example.data.local.BundledQuranAssetLoader
import com.example.data.local.WirdSeedData
import com.example.data.local.dao.AyahBookmarkDao
import com.example.data.local.dao.PlanConfigDao
import com.example.data.local.dao.SurahAyahDao
import com.example.data.local.dao.SurahPlanDao
import com.example.data.local.entity.AyahBookmarkEntity
import com.example.data.local.entity.PlanConfigEntity
import com.example.data.local.entity.SurahAyahEntity
import com.example.data.local.entity.SurahPlanEntity
import com.example.data.remote.QuranTextService
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

class WirdRepository(
    private val surahPlanDao: SurahPlanDao,
    private val planConfigDao: PlanConfigDao,
    private val surahAyahDao: SurahAyahDao,
    private val ayahBookmarkDao: AyahBookmarkDao,
    private val bundledQuranLoader: BundledQuranAssetLoader? = null,
    private val quranTextService: QuranTextService = QuranTextService()
) {
    val allSurahs: Flow<List<SurahPlanEntity>> = surahPlanDao.getAllSurahs()
    val planConfig: Flow<PlanConfigEntity?> = planConfigDao.getConfig()
    val allBookmarks: Flow<List<AyahBookmarkEntity>> = ayahBookmarkDao.getAllBookmarks()

    fun observeSurahAyahs(surahNumber: Int): Flow<List<SurahAyahEntity>> =
        surahAyahDao.getAyahsForSurah(surahNumber)

    suspend fun ensureSurahTextLoaded(surahNumber: Int): Result<Unit> {
        if (surahAyahDao.getAyahCountForSurah(surahNumber) > 0) {
            return Result.success(Unit)
        }
        // Offline-first: load verified Uthmani text from bundled APK assets before any network request
        val bundled = bundledQuranLoader?.loadSurahAyahs(surahNumber).orEmpty()
        if (bundled.isNotEmpty()) {
            surahAyahDao.insertAll(bundled)
            return Result.success(Unit)
        }
        val fetched = quranTextService.fetchSurahAyahs(surahNumber)
        return fetched.fold(
            onSuccess = { ayahs ->
                if (ayahs.isNotEmpty()) {
                    surahAyahDao.insertAll(ayahs)
                    Result.success(Unit)
                } else {
                    Result.failure(IllegalStateException("Empty surah response"))
                }
            },
            onFailure = { err -> Result.failure(err) }
        )
    }

    suspend fun ensureInitialized() {
        if (surahPlanDao.getSurahCount() == 0) {
            surahPlanDao.insertAll(WirdSeedData.createDefault14DayPlan())
        }
        if (planConfigDao.getConfigOnce() == null) {
            planConfigDao.upsertConfig(
                PlanConfigEntity(
                    id = 1,
                    startDateEpochDay = currentEpochDay(),
                    reminderEnabled = false,
                    reminderHour = 20,
                    reminderMinute = 0,
                    completedCycles = 0,
                    themeMode = PlanConfigEntity.THEME_SYSTEM
                )
            )
        }
        if (bundledQuranLoader != null && surahAyahDao.getTotalAyahCount() < BundledQuranAssetLoader.EXPECTED_TOTAL_AYAHS) {
            val allBundledAyahs = bundledQuranLoader.loadAllSurahAyahs()
            if (allBundledAyahs.isNotEmpty()) {
                surahAyahDao.insertAll(allBundledAyahs)
            }
        }
    }

    suspend fun getConfigOnce(): PlanConfigEntity? = planConfigDao.getConfigOnce()

    suspend fun toggleSurahCompleted(surahNumber: Int, isCompleted: Boolean) {
        val timestamp = if (isCompleted) System.currentTimeMillis() else null
        surahPlanDao.updateSurahCompletion(surahNumber, isCompleted, timestamp)
    }

    suspend fun setDayCompleted(dayNumber: Int, isCompleted: Boolean) {
        val timestamp = if (isCompleted) System.currentTimeMillis() else null
        surahPlanDao.updateDayCompletion(dayNumber, isCompleted, timestamp)
    }

    suspend fun moveSurahToDay(surahNumber: Int, newDayNumber: Int) {
        val clampedDay = newDayNumber.coerceIn(1, WirdSeedData.TOTAL_DAYS)
        surahPlanDao.updateSurahDay(surahNumber, clampedDay)
    }

    suspend fun updateSurahNote(surahNumber: Int, note: String) {
        surahPlanDao.updateSurahNote(surahNumber, note.trim())
    }

    suspend fun restoreDefaultDistribution() {
        val existingByNumber = surahPlanDao.getAllSurahsOnce().associateBy { it.surahNumber }
        val defaults = WirdSeedData.createDefault14DayPlan().map { seed ->
            val current = existingByNumber[seed.surahNumber]
            if (current != null) {
                seed.copy(
                    isCompleted = current.isCompleted,
                    completedAtMillis = current.completedAtMillis,
                    readingNote = current.readingNote
                )
            } else {
                seed
            }
        }
        surahPlanDao.insertAll(defaults)
    }

    suspend fun startNewCycle() {
        val currentSurahs = surahPlanDao.getAllSurahsOnce()
        val allDone = currentSurahs.isNotEmpty() && currentSurahs.all { it.isCompleted }
        val currentConfig = planConfigDao.getConfigOnce() ?: PlanConfigEntity(
            startDateEpochDay = currentEpochDay()
        )
        surahPlanDao.resetAllCompletions()
        planConfigDao.upsertConfig(
            currentConfig.copy(
                startDateEpochDay = currentEpochDay(),
                completedCycles = if (allDone) currentConfig.completedCycles + 1 else currentConfig.completedCycles
            )
        )
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        val currentConfig = planConfigDao.getConfigOnce() ?: PlanConfigEntity(
            startDateEpochDay = currentEpochDay()
        )
        planConfigDao.upsertConfig(
            currentConfig.copy(
                reminderEnabled = enabled,
                reminderHour = hour.coerceIn(0, 23),
                reminderMinute = minute.coerceIn(0, 59)
            )
        )
    }

    suspend fun updateThemeMode(themeMode: String) {
        val currentConfig = planConfigDao.getConfigOnce() ?: PlanConfigEntity(
            startDateEpochDay = currentEpochDay()
        )
        planConfigDao.upsertConfig(currentConfig.copy(themeMode = themeMode))
    }

    suspend fun updateReaderFontSize(fontSizeSp: Float) {
        val currentConfig = planConfigDao.getConfigOnce() ?: PlanConfigEntity(
            startDateEpochDay = currentEpochDay()
        )
        planConfigDao.upsertConfig(
            currentConfig.copy(readerFontSizeSp = fontSizeSp.coerceIn(18f, 42f))
        )
    }

    suspend fun updateReaderFontKey(fontKey: String) {
        val currentConfig = planConfigDao.getConfigOnce() ?: PlanConfigEntity(
            startDateEpochDay = currentEpochDay()
        )
        planConfigDao.upsertConfig(
            currentConfig.copy(readerFontKey = fontKey)
        )
    }

    suspend fun toggleAyahBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        surahArabicName: String,
        ayahText: String
    ): Boolean {
        return if (ayahBookmarkDao.isBookmarked(surahNumber, ayahNumber)) {
            ayahBookmarkDao.deleteBookmark(surahNumber, ayahNumber)
            false
        } else {
            val preview = ayahText.trim().let {
                if (it.length > 90) it.take(90) + "…" else it
            }
            ayahBookmarkDao.upsertBookmark(
                AyahBookmarkEntity(
                    surahNumber = surahNumber,
                    ayahNumber = ayahNumber,
                    surahArabicName = surahArabicName,
                    ayahPreviewText = preview,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            true
        }
    }

    suspend fun removeAyahBookmark(surahNumber: Int, ayahNumber: Int) {
        ayahBookmarkDao.deleteBookmark(surahNumber, ayahNumber)
    }

    companion object {
        fun currentEpochDay(): Long {
            val offsetMillis = java.util.TimeZone.getDefault().getOffset(System.currentTimeMillis())
            return TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() + offsetMillis)
        }
    }
}
