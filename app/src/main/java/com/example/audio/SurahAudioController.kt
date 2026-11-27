package com.example.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class QuranReciter(
    val storageKey: String,
    val arabicName: String,
    private val primaryBaseUrl: String,
    private val fallbackBaseUrl: String
) {
    SAAD_AL_GHAMDI(
        storageKey = "saad_al_ghamdi",
        arabicName = "سعد الغامدي",
        primaryBaseUrl = "https://server7.mp3quran.net/s_gmd/",
        fallbackBaseUrl = "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/"
    ),
    ALAFASY(
        storageKey = "alafasy",
        arabicName = "مشاري العفاسي",
        primaryBaseUrl = "https://server8.mp3quran.net/afs/",
        fallbackBaseUrl = "https://cdn.mp3quran.net/audio/mishary-alafasy/r1/"
    ),
    HUSARY(
        storageKey = "husary",
        arabicName = "محمود خليل الحصري",
        primaryBaseUrl = "https://server13.mp3quran.net/husr/",
        fallbackBaseUrl = "https://cdn.mp3quran.net/audio/mahmoud-husary/r1/"
    ),
    ABDUL_BASIT(
        storageKey = "abdul_basit",
        arabicName = "عبد الباسط عبد الصمد",
        primaryBaseUrl = "https://server7.mp3quran.net/basit/",
        fallbackBaseUrl = "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/"
    );

    fun primaryUrl(surahNumber: Int): String =
        "$primaryBaseUrl${surahNumber.toString().padStart(3, '0')}.mp3"

    fun fallbackUrl(surahNumber: Int): String =
        "$fallbackBaseUrl${surahNumber.toString().padStart(3, '0')}.mp3"

    companion object {
        fun fromStorageKey(key: String?): QuranReciter =
            entries.firstOrNull { it.storageKey == key } ?: SAAD_AL_GHAMDI
    }
}

data class SurahAudioState(
    val loadedSurahNumber: Int? = null,
    val selectedReciter: QuranReciter = QuranReciter.SAAD_AL_GHAMDI,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isPlayingOffline: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val activeAyahNumber: Int? = null, // 0 = Basmalah, 1..N = verse, null = idle
    val isAutoFollowEnabled: Boolean = true,
    val hasError: Boolean = false
)

class SurahAudioController(
    context: Context,
    private val scope: CoroutineScope,
    private val downloadManager: RecitationDownloadManager,
    private val timingRepository: AyahTimingRepository
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var exoPlayer: ExoPlayer? = null
    private var progressJob: Job? = null
    private var timingLoadJob: Job? = null
    private var triedFallbackForCurrentLoad = false
    private var pendingSeekMs: Long? = null

    @Volatile
    private var currentTimings: List<AyahTiming> = emptyList()
    private var currentTimingsSurah: Int? = null
    private var currentTimingsReciter: QuranReciter? = null

    private val _state = MutableStateFlow(
        SurahAudioState(
            selectedReciter = QuranReciter.fromStorageKey(prefs.getString(KEY_SELECTED_RECITER, null))
        )
    )
    val state: StateFlow<SurahAudioState> = _state.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()
                syncProgressOnce()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _state.update { it.copy(isBuffering = true, hasError = false) }
                }

                Player.STATE_READY -> {
                    val player = exoPlayer
                    val pending = pendingSeekMs
                    if (pending != null && player != null) {
                        pendingSeekMs = null
                        val duration = player.duration.coerceAtLeast(0L)
                        val clamped = if (duration > 0L) pending.coerceIn(0L, duration) else pending
                        player.seekTo(clamped)
                    }
                    val pos = player?.currentPosition?.coerceAtLeast(0L) ?: _state.value.currentPositionMs
                    val dur = player?.duration?.coerceAtLeast(0L) ?: _state.value.durationMs
                    val activeAyah = resolveActiveAyah(pos, player?.isPlaying == true || pos > 0L)
                    _state.update {
                        it.copy(
                            isBuffering = false,
                            hasError = false,
                            durationMs = dur,
                            currentPositionMs = pos,
                            activeAyahNumber = activeAyah
                        )
                    }
                }

                Player.STATE_ENDED -> {
                    stopProgressUpdates()
                    pendingSeekMs = null
                    val duration = exoPlayer?.duration?.coerceAtLeast(0L) ?: _state.value.durationMs
                    _state.update {
                        it.copy(
                            isPlaying = false,
                            isBuffering = false,
                            currentPositionMs = duration,
                            durationMs = duration,
                            activeAyahNumber = null
                        )
                    }
                }

                Player.STATE_IDLE -> {
                    _state.update { it.copy(isBuffering = false) }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val currentState = _state.value
            val surahNum = currentState.loadedSurahNumber
            if (!currentState.isPlayingOffline && !triedFallbackForCurrentLoad && surahNum != null) {
                triedFallbackForCurrentLoad = true
                loadMedia(
                    surahNumber = surahNum,
                    mediaItem = MediaItem.fromUri(currentState.selectedReciter.fallbackUrl(surahNum)),
                    isOffline = false,
                    autoPlay = true,
                    initialSeekMs = pendingSeekMs
                )
            } else {
                stopProgressUpdates()
                pendingSeekMs = null
                _state.update {
                    it.copy(
                        isPlaying = false,
                        isBuffering = false,
                        activeAyahNumber = null,
                        hasError = true
                    )
                }
            }
        }
    }

    private fun getOrCreatePlayer(): ExoPlayer {
        return exoPlayer ?: ExoPlayer.Builder(appContext)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { player ->
                player.addListener(playerListener)
                exoPlayer = player
            }
    }

    fun onSurahOpened(surahNumber: Int) {
        loadTimingsAsync(_state.value.selectedReciter, surahNumber)
        if (_state.value.loadedSurahNumber != surahNumber) {
            stopAndClearPlayer()
            _state.update {
                it.copy(
                    loadedSurahNumber = null,
                    isPlaying = false,
                    isBuffering = false,
                    isPlayingOffline = false,
                    currentPositionMs = 0L,
                    durationMs = 0L,
                    activeAyahNumber = null,
                    hasError = false
                )
            }
        }
    }

    fun togglePlayPause(surahNumber: Int) {
        val current = _state.value
        val player = getOrCreatePlayer()
        loadTimingsAsync(current.selectedReciter, surahNumber)

        if (current.loadedSurahNumber != surahNumber || current.hasError) {
            playSurah(surahNumber, current.selectedReciter, initialSeekMs = null)
            return
        }

        if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0L)
            player.play()
        } else if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun playFromAyah(surahNumber: Int, ayahNumber: Int) {
        val reciter = _state.value.selectedReciter
        scope.launch {
            val timings = timingRepository.getTimings(reciter, surahNumber)
            currentTimings = timings
            currentTimingsSurah = surahNumber
            currentTimingsReciter = reciter

            val targetMs = timingRepository.findStartMsForAyah(timings, ayahNumber) ?: 0L
            val current = _state.value
            val player = getOrCreatePlayer()

            if (current.loadedSurahNumber == surahNumber && !current.hasError && player.playbackState != Player.STATE_IDLE) {
                val duration = player.duration.coerceAtLeast(0L)
                val clamped = if (duration > 0L) targetMs.coerceIn(0L, duration) else targetMs
                player.seekTo(clamped)
                player.play()
                _state.update {
                    it.copy(
                        currentPositionMs = clamped,
                        activeAyahNumber = ayahNumber
                    )
                }
            } else {
                playSurah(surahNumber, reciter, initialSeekMs = targetMs)
                _state.update { it.copy(activeAyahNumber = ayahNumber) }
            }
        }
    }

    fun toggleAutoFollow() {
        _state.update { it.copy(isAutoFollowEnabled = !it.isAutoFollowEnabled) }
    }

    fun selectReciter(reciter: QuranReciter, surahNumber: Int?) {
        if (_state.value.selectedReciter == reciter) return
        prefs.edit().putString(KEY_SELECTED_RECITER, reciter.storageKey).apply()

        val wasPlaying = _state.value.isPlaying || _state.value.isBuffering
        val previousAyah = _state.value.activeAyahNumber
        _state.update {
            it.copy(
                selectedReciter = reciter,
                loadedSurahNumber = null,
                isPlayingOffline = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                activeAyahNumber = null,
                hasError = false
            )
        }
        if (surahNumber != null) {
            if (wasPlaying) {
                if (previousAyah != null && previousAyah >= 1) {
                    playFromAyah(surahNumber, previousAyah)
                } else {
                    loadTimingsAsync(reciter, surahNumber)
                    playSurah(surahNumber, reciter, initialSeekMs = null)
                }
            } else {
                loadTimingsAsync(reciter, surahNumber)
                stopAndClearPlayer()
            }
        } else {
            stopAndClearPlayer()
        }
    }

    fun onRecitationDeleted(reciter: QuranReciter, surahNumber: Int) {
        val current = _state.value
        if (current.selectedReciter == reciter && current.loadedSurahNumber == surahNumber && current.isPlayingOffline) {
            stopPlayback()
        }
    }

    fun onAllRecitationsDeleted() {
        if (_state.value.isPlayingOffline) {
            stopPlayback()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        val target = positionMs.coerceIn(0L, duration)
        player.seekTo(target)
        val activeAyah = resolveActiveAyah(target, player.isPlaying || target > 0L)
        _state.update {
            it.copy(
                currentPositionMs = target,
                activeAyahNumber = activeAyah
            )
        }
    }

    fun seekBy(deltaMs: Long) {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        if (duration <= 0L) return
        val target = (player.currentPosition + deltaMs).coerceIn(0L, duration)
        player.seekTo(target)
        val activeAyah = resolveActiveAyah(target, player.isPlaying || target > 0L)
        _state.update {
            it.copy(
                currentPositionMs = target,
                activeAyahNumber = activeAyah
            )
        }
    }

    fun stopPlayback() {
        stopAndClearPlayer()
        pendingSeekMs = null
        _state.update {
            it.copy(
                loadedSurahNumber = null,
                isPlaying = false,
                isBuffering = false,
                isPlayingOffline = false,
                currentPositionMs = 0L,
                durationMs = 0L,
                activeAyahNumber = null,
                hasError = false
            )
        }
    }

    fun release() {
        stopProgressUpdates()
        timingLoadJob?.cancel()
        exoPlayer?.removeListener(playerListener)
        exoPlayer?.release()
        exoPlayer = null
    }

    private fun loadTimingsAsync(reciter: QuranReciter, surahNumber: Int) {
        if (currentTimingsReciter == reciter && currentTimingsSurah == surahNumber && currentTimings.isNotEmpty()) {
            return
        }
        timingLoadJob?.cancel()
        timingLoadJob = scope.launch {
            val loaded = timingRepository.getTimings(reciter, surahNumber)
            currentTimings = loaded
            currentTimingsReciter = reciter
            currentTimingsSurah = surahNumber
            val player = exoPlayer
            if (player != null && _state.value.loadedSurahNumber == surahNumber && (player.isPlaying || player.currentPosition > 0L)) {
                val activeAyah = resolveActiveAyah(player.currentPosition.coerceAtLeast(0L), true)
                _state.update { it.copy(activeAyahNumber = activeAyah) }
            }
        }
    }

    private fun playSurah(surahNumber: Int, reciter: QuranReciter, initialSeekMs: Long?) {
        triedFallbackForCurrentLoad = false
        val localFile = downloadManager.getLocalAudioFile(reciter, surahNumber)
        if (localFile != null) {
            loadMedia(
                surahNumber = surahNumber,
                mediaItem = MediaItem.fromUri(Uri.fromFile(localFile)),
                isOffline = true,
                autoPlay = true,
                initialSeekMs = initialSeekMs
            )
        } else {
            loadMedia(
                surahNumber = surahNumber,
                mediaItem = MediaItem.fromUri(reciter.primaryUrl(surahNumber)),
                isOffline = false,
                autoPlay = true,
                initialSeekMs = initialSeekMs
            )
        }
    }

    private fun loadMedia(
        surahNumber: Int,
        mediaItem: MediaItem,
        isOffline: Boolean,
        autoPlay: Boolean,
        initialSeekMs: Long?
    ) {
        val player = getOrCreatePlayer()
        pendingSeekMs = initialSeekMs
        _state.update {
            it.copy(
                loadedSurahNumber = surahNumber,
                isBuffering = !isOffline,
                isPlayingOffline = isOffline,
                hasError = false,
                currentPositionMs = initialSeekMs ?: 0L,
                durationMs = 0L
            )
        }
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = autoPlay
    }

    private fun stopAndClearPlayer() {
        stopProgressUpdates()
        pendingSeekMs = null
        exoPlayer?.stop()
        exoPlayer?.clearMediaItems()
    }

    private fun startProgressUpdates() {
        if (progressJob?.isActive == true) return
        progressJob = scope.launch {
            while (isActive) {
                syncProgressOnce()
                delay(150L)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun syncProgressOnce() {
        val player = exoPlayer ?: return
        val duration = player.duration.coerceAtLeast(0L)
        val position = player.currentPosition.coerceIn(0L, duration.coerceAtLeast(1L))
        val effectivePos = if (duration > 0L) position else 0L
        val isActivePlayback = player.isPlaying || (effectivePos > 0L && player.playbackState != Player.STATE_ENDED)
        val activeAyah = resolveActiveAyah(effectivePos, isActivePlayback)
        _state.update {
            it.copy(
                currentPositionMs = effectivePos,
                durationMs = duration,
                activeAyahNumber = activeAyah
            )
        }
    }

    private fun resolveActiveAyah(positionMs: Long, isActivePlayback: Boolean): Int? {
        if (!isActivePlayback) return null
        val loadedSurah = _state.value.loadedSurahNumber ?: return null
        if (currentTimingsSurah != loadedSurah || currentTimingsReciter != _state.value.selectedReciter) {
            return null
        }
        return timingRepository.findActiveAyah(currentTimings, positionMs)
    }

    companion object {
        private const val PREFS_NAME = "wird_audio_prefs"
        private const val KEY_SELECTED_RECITER = "selected_reciter"
    }
}
