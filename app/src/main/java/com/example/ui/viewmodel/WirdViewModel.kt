package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AyahTimingRepository
import com.example.audio.QuranReciter
import com.example.audio.RecitationDownloadManager
import com.example.audio.RecitationStorageState
import com.example.audio.SurahAudioController
import com.example.audio.SurahAudioState
import com.example.data.local.WirdSeedData
import com.example.data.local.entity.AyahBookmarkEntity
import com.example.data.local.entity.SurahAyahEntity
import com.example.data.local.entity.SurahPlanEntity
import com.example.data.repository.WirdRepository
import com.example.reminder.DailyReminderScheduler
import com.example.ui.theme.QuranFontOption
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.ceil

enum class WirdTab {
    TODAY,
    PLAN,
    REMINDER
}

data class DayPlanUiModel(
    val dayNumber: Int,
    val surahs: List<SurahPlanEntity>,
    val totalAyahs: Int,
    val completedSurahs: Int,
    val completedAyahs: Int,
    val startPage: Int,
    val endPage: Int,
    val estimatedMinutes: Int,
    val isCompleted: Boolean,
    val isCurrentDay: Boolean,
    val completionRatio: Float
)

data class SurahReaderUiState(
    val surah: SurahPlanEntity,
    val ayahs: List<SurahAyahEntity>,
    val isLoading: Boolean,
    val errorMessage: String?,
    val nextSurahInDay: SurahPlanEntity?,
    val fontSizeSp: Float,
    val fontOption: QuranFontOption,
    val isMushafMode: Boolean,
    val bookmarkedAyahNumbers: Set<Int>,
    val targetScrollAyahNumber: Int?
)

data class WirdUiState(
    val days: List<DayPlanUiModel>,
    val selectedDayNumber: Int,
    val currentPlanDay: Int,
    val totalSurahs: Int,
    val completedSurahs: Int,
    val totalAyahs: Int,
    val completedAyahs: Int,
    val completedDaysCount: Int,
    val overallProgress: Float,
    val juzTabarakProgress: Float,
    val juzAmmaProgress: Float,
    val streakDays: Int,
    val completedCycles: Int,
    val isKhatmahComplete: Boolean,
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val isNextReminderToday: Boolean,
    val themeMode: String,
    val readerFontSizeSp: Float,
    val readerFont: QuranFontOption,
    val bookmarks: List<AyahBookmarkEntity>,
    val selectedSurah: SurahPlanEntity?,
    val readerState: SurahReaderUiState?
) {
    val selectedDayPlan: DayPlanUiModel?
        get() = days.firstOrNull { it.dayNumber == selectedDayNumber }
}

sealed interface WirdScreenState {
    data object Loading : WirdScreenState
    data class Ready(val data: WirdUiState) : WirdScreenState
}

@OptIn(ExperimentalCoroutinesApi::class)
class WirdViewModel(
    appContext: Context,
    private val repository: WirdRepository,
    private val reminderScheduler: DailyReminderScheduler
) : ViewModel() {

    private val downloadManager = RecitationDownloadManager(appContext, viewModelScope)
    private val timingRepository = AyahTimingRepository(appContext)
    private val audioController = SurahAudioController(appContext, viewModelScope, downloadManager, timingRepository)
    val audioState: StateFlow<SurahAudioState> = audioController.state
    val recitationStorageState: StateFlow<RecitationStorageState> = downloadManager.storageState

    private val selectedDayOverride = MutableStateFlow<Int?>(null)
    private val selectedSurahDetailNumber = MutableStateFlow<Int?>(null)
    private val readingSurahNumber = MutableStateFlow<Int?>(null)
    private val targetScrollAyahNumber = MutableStateFlow<Int?>(null)
    private val isReaderLoading = MutableStateFlow(false)
    private val readerError = MutableStateFlow<String?>(null)
    private val readerMushafMode = MutableStateFlow(true)

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
            reminderScheduler.ensureNotificationChannel()
            val config = repository.getConfigOnce()
            if (config != null && config.reminderEnabled) {
                reminderScheduler.scheduleDailyReminder(config.reminderHour, config.reminderMinute)
            }
        }
    }

    private val activeSurahAyahs = readingSurahNumber.flatMapLatest { surahNum ->
        if (surahNum == null) flowOf(emptyList()) else repository.observeSurahAyahs(surahNum)
    }

    private data class ReaderControlTuple(
        val surahNumber: Int?,
        val ayahs: List<SurahAyahEntity>,
        val isLoading: Boolean,
        val error: String?,
        val isMushafMode: Boolean,
        val targetAyah: Int?,
        val bookmarks: List<AyahBookmarkEntity>
    )

    private val readerControlsFlow = combine(
        readingSurahNumber,
        activeSurahAyahs,
        isReaderLoading,
        readerError,
        readerMushafMode
    ) { surahNum, ayahs, loading, err, mushafMode ->
        ReaderControlTuple(surahNum, ayahs, loading, err, mushafMode, null, emptyList())
    }.combine(targetScrollAyahNumber) { tuple, targetAyah ->
        tuple.copy(targetAyah = targetAyah)
    }.combine(repository.allBookmarks) { tuple, allBookmarks ->
        tuple.copy(bookmarks = allBookmarks)
    }

    val screenState: StateFlow<WirdScreenState> = combine(
        repository.allSurahs,
        repository.planConfig,
        selectedDayOverride,
        selectedSurahDetailNumber,
        readerControlsFlow
    ) { surahs, config, dayOverride, surahDetailNum, readerTuple ->
        if (surahs.isEmpty() || config == null) {
            WirdScreenState.Loading
        } else {
            val todayEpoch = WirdRepository.currentEpochDay()
            val elapsedDays = (todayEpoch - config.startDateEpochDay).toInt()
            val calendarPlanDay = (elapsedDays + 1).coerceIn(1, WirdSeedData.TOTAL_DAYS)

            val grouped = surahs.groupBy { it.dayNumber }
            val dayModels = (1..WirdSeedData.TOTAL_DAYS).map { dayNum ->
                val daySurahs = grouped[dayNum].orEmpty().sortedBy { it.surahNumber }
                val totalAyahs = daySurahs.sumOf { it.ayahCount }
                val completedSurahCount = daySurahs.count { it.isCompleted }
                val completedAyahCount = daySurahs.filter { it.isCompleted }.sumOf { it.ayahCount }
                val isComplete = daySurahs.isNotEmpty() && completedSurahCount == daySurahs.size
                val ratio = if (daySurahs.isEmpty()) 0f else completedSurahCount.toFloat() / daySurahs.size
                val startPage = daySurahs.minOfOrNull { it.startPage } ?: 562
                val endPage = daySurahs.maxOfOrNull { it.startPage } ?: startPage
                val estimatedMinutes = ceil(totalAyahs / 11.0).toInt().coerceAtLeast(if (daySurahs.isEmpty()) 0 else 4)

                DayPlanUiModel(
                    dayNumber = dayNum,
                    surahs = daySurahs,
                    totalAyahs = totalAyahs,
                    completedSurahs = completedSurahCount,
                    completedAyahs = completedAyahCount,
                    startPage = startPage,
                    endPage = endPage,
                    estimatedMinutes = estimatedMinutes,
                    isCompleted = isComplete,
                    isCurrentDay = dayNum == calendarPlanDay,
                    completionRatio = ratio
                )
            }

            val firstIncompleteDay = dayModels.firstOrNull { !it.isCompleted }?.dayNumber ?: calendarPlanDay
            val activeSelectedDay = (dayOverride ?: firstIncompleteDay).coerceIn(1, WirdSeedData.TOTAL_DAYS)

            val totalSurahs = surahs.size
            val completedSurahs = surahs.count { it.isCompleted }
            val totalAyahs = surahs.sumOf { it.ayahCount }
            val completedAyahs = surahs.filter { it.isCompleted }.sumOf { it.ayahCount }
            val completedDaysCount = dayModels.count { it.isCompleted }
            val overallProgress = if (totalSurahs == 0) 0f else completedSurahs.toFloat() / totalSurahs

            val juz29 = surahs.filter { it.juzNumber == 29 }
            val juz30 = surahs.filter { it.juzNumber == 30 }
            val juz29Progress = if (juz29.isEmpty()) 0f else juz29.count { it.isCompleted }.toFloat() / juz29.size
            val juz30Progress = if (juz30.isEmpty()) 0f else juz30.count { it.isCompleted }.toFloat() / juz30.size

            var streak = 0
            for (day in dayModels) {
                if (day.isCompleted) {
                    streak++
                } else if (day.dayNumber <= calendarPlanDay) {
                    streak = 0
                } else {
                    break
                }
            }

            val selectedReaderFont = QuranFontOption.fromStorageKey(config.readerFontKey)

            val activeReaderState = readerTuple.surahNumber?.let { activeNum ->
                val surahEntity = surahs.firstOrNull { it.surahNumber == activeNum }
                if (surahEntity != null) {
                    val sameDaySurahs = grouped[surahEntity.dayNumber].orEmpty().sortedBy { it.surahNumber }
                    val idx = sameDaySurahs.indexOfFirst { it.surahNumber == activeNum }
                    val nextInDay = if (idx >= 0 && idx + 1 < sameDaySurahs.size) sameDaySurahs[idx + 1] else null
                    val bookmarkedInSurah = readerTuple.bookmarks
                        .filter { it.surahNumber == activeNum }
                        .map { it.ayahNumber }
                        .toSet()

                    SurahReaderUiState(
                        surah = surahEntity,
                        ayahs = readerTuple.ayahs,
                        isLoading = readerTuple.isLoading,
                        errorMessage = readerTuple.error,
                        nextSurahInDay = nextInDay,
                        fontSizeSp = config.readerFontSizeSp,
                        fontOption = selectedReaderFont,
                        isMushafMode = readerTuple.isMushafMode,
                        bookmarkedAyahNumbers = bookmarkedInSurah,
                        targetScrollAyahNumber = readerTuple.targetAyah
                    )
                } else {
                    null
                }
            }

            WirdScreenState.Ready(
                WirdUiState(
                    days = dayModels,
                    selectedDayNumber = activeSelectedDay,
                    currentPlanDay = calendarPlanDay,
                    totalSurahs = totalSurahs,
                    completedSurahs = completedSurahs,
                    totalAyahs = totalAyahs,
                    completedAyahs = completedAyahs,
                    completedDaysCount = completedDaysCount,
                    overallProgress = overallProgress,
                    juzTabarakProgress = juz29Progress,
                    juzAmmaProgress = juz30Progress,
                    streakDays = streak,
                    completedCycles = config.completedCycles,
                    isKhatmahComplete = totalSurahs > 0 && completedSurahs == totalSurahs,
                    reminderEnabled = config.reminderEnabled,
                    reminderHour = config.reminderHour,
                    reminderMinute = config.reminderMinute,
                    isNextReminderToday = reminderScheduler.isNextReminderToday(
                        config.reminderHour,
                        config.reminderMinute
                    ),
                    themeMode = config.themeMode,
                    readerFontSizeSp = config.readerFontSizeSp,
                    readerFont = selectedReaderFont,
                    bookmarks = readerTuple.bookmarks,
                    selectedSurah = surahDetailNum?.let { num -> surahs.firstOrNull { it.surahNumber == num } },
                    readerState = activeReaderState
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = WirdScreenState.Loading
    )

    fun selectDay(dayNumber: Int) {
        selectedDayOverride.value = dayNumber.coerceIn(1, WirdSeedData.TOTAL_DAYS)
    }

    fun openSurahReader(surahNumber: Int) {
        audioController.onSurahOpened(surahNumber)
        targetScrollAyahNumber.value = null
        readingSurahNumber.value = surahNumber
        loadSurahText(surahNumber)
    }

    fun openSurahAtAyah(surahNumber: Int, ayahNumber: Int) {
        audioController.onSurahOpened(surahNumber)
        // Switch to verse-by-verse mode so the LazyColumn scrolls accurately straight to the exact verse
        readerMushafMode.value = false
        targetScrollAyahNumber.value = ayahNumber
        readingSurahNumber.value = surahNumber
        loadSurahText(surahNumber)
    }

    fun consumeTargetScrollAyah() {
        targetScrollAyahNumber.value = null
    }

    fun closeSurahReader() {
        audioController.stopPlayback()
        readingSurahNumber.value = null
        targetScrollAyahNumber.value = null
        readerError.value = null
        isReaderLoading.value = false
    }

    fun toggleAudioPlayPause(surahNumber: Int) {
        audioController.togglePlayPause(surahNumber)
    }

    fun seekAudioTo(positionMs: Long) {
        audioController.seekTo(positionMs)
    }

    fun rewindAudio10s() {
        audioController.seekBy(-10_000L)
    }

    fun forwardAudio10s() {
        audioController.seekBy(10_000L)
    }

    fun selectReciter(reciter: QuranReciter, surahNumber: Int? = readingSurahNumber.value) {
        audioController.selectReciter(reciter, surahNumber)
    }

    fun playFromAyah(surahNumber: Int, ayahNumber: Int) {
        audioController.playFromAyah(surahNumber, ayahNumber)
    }

    fun toggleAudioAutoFollow() {
        audioController.toggleAutoFollow()
    }

    fun downloadRecitation(surahNumber: Int) {
        downloadManager.downloadSurah(audioState.value.selectedReciter, surahNumber)
    }

    fun cancelRecitationDownload(surahNumber: Int) {
        downloadManager.cancelDownload(audioState.value.selectedReciter, surahNumber)
    }

    fun deleteRecitation(surahNumber: Int) {
        val reciter = audioState.value.selectedReciter
        audioController.onRecitationDeleted(reciter, surahNumber)
        downloadManager.deleteSurah(reciter, surahNumber)
    }

    fun downloadAllRecitationsForReciter(reciter: QuranReciter) {
        downloadManager.downloadAllForReciter(reciter)
    }

    fun cancelBulkRecitationDownload() {
        downloadManager.cancelBulkDownload()
    }

    fun deleteAllSavedRecitations() {
        audioController.onAllRecitationsDeleted()
        downloadManager.deleteAllRecitations()
    }

    override fun onCleared() {
        super.onCleared()
        audioController.release()
    }

    fun retryLoadSurahText() {
        readingSurahNumber.value?.let { loadSurahText(it) }
    }

    private fun loadSurahText(surahNumber: Int) {
        viewModelScope.launch {
            isReaderLoading.value = true
            readerError.value = null
            val result = repository.ensureSurahTextLoaded(surahNumber)
            if (readingSurahNumber.value == surahNumber) {
                isReaderLoading.value = false
                if (result.isFailure) {
                    readerError.value = result.exceptionOrNull()?.message ?: "error"
                }
            }
        }
    }

    fun toggleAyahBookmark(
        surahNumber: Int,
        ayahNumber: Int,
        surahArabicName: String,
        ayahText: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val isNowBookmarked = repository.toggleAyahBookmark(
                surahNumber = surahNumber,
                ayahNumber = ayahNumber,
                surahArabicName = surahArabicName,
                ayahText = ayahText
            )
            onResult(isNowBookmarked)
        }
    }

    fun removeAyahBookmark(surahNumber: Int, ayahNumber: Int) {
        viewModelScope.launch {
            repository.removeAyahBookmark(surahNumber, ayahNumber)
        }
    }

    fun setReaderFontSize(fontSizeSp: Float) {
        viewModelScope.launch {
            repository.updateReaderFontSize(fontSizeSp)
        }
    }

    fun setReaderFont(fontOption: QuranFontOption) {
        viewModelScope.launch {
            repository.updateReaderFontKey(fontOption.storageKey)
        }
    }

    fun increaseFontSize() {
        viewModelScope.launch {
            val current = repository.getConfigOnce()?.readerFontSizeSp ?: 26f
            repository.updateReaderFontSize(current + 2f)
        }
    }

    fun decreaseFontSize() {
        viewModelScope.launch {
            val current = repository.getConfigOnce()?.readerFontSizeSp ?: 26f
            repository.updateReaderFontSize(current - 2f)
        }
    }

    fun setMushafMode(isMushaf: Boolean) {
        readerMushafMode.value = isMushaf
    }

    fun toggleSurah(surahNumber: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleSurahCompleted(surahNumber, isCompleted)
        }
    }

    fun setDayCompleted(dayNumber: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setDayCompleted(dayNumber, isCompleted)
        }
    }

    fun openSurahDetail(surahNumber: Int) {
        selectedSurahDetailNumber.value = surahNumber
    }

    fun closeSurahDetail() {
        selectedSurahDetailNumber.value = null
    }

    fun moveSurahToDay(surahNumber: Int, newDayNumber: Int) {
        viewModelScope.launch {
            repository.moveSurahToDay(surahNumber, newDayNumber)
        }
    }

    fun saveSurahNote(surahNumber: Int, note: String) {
        viewModelScope.launch {
            repository.updateSurahNote(surahNumber, note)
        }
    }

    fun startNewCycle() {
        viewModelScope.launch {
            repository.startNewCycle()
            selectedDayOverride.value = 1
        }
    }

    fun restoreDefaultDistribution() {
        viewModelScope.launch {
            repository.restoreDefaultDistribution()
        }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getConfigOnce()
            val hour = current?.reminderHour ?: 20
            val minute = current?.reminderMinute ?: 0
            repository.updateReminder(enabled, hour, minute)
            if (enabled) {
                reminderScheduler.scheduleDailyReminder(hour, minute)
            } else {
                reminderScheduler.cancelDailyReminder()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            val current = repository.getConfigOnce()
            val enabled = current?.reminderEnabled ?: true
            repository.updateReminder(enabled, hour, minute)
            if (enabled) {
                reminderScheduler.scheduleDailyReminder(hour, minute)
            }
        }
    }

    fun sendTestNotification(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val payload = reminderScheduler.buildDynamicDailyPayload()
            val shown = reminderScheduler.showReminderNotification(
                title = payload.title,
                body = payload.body
            )
            onResult(shown)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            repository.updateThemeMode(mode)
        }
    }
}

class WirdViewModelFactory(
    private val appContext: Context,
    private val repository: WirdRepository,
    private val reminderScheduler: DailyReminderScheduler
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WirdViewModel::class.java)) {
            return WirdViewModel(appContext, repository, reminderScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
