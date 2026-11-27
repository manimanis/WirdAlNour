package com.example.audio

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

data class RecitationStorageState(
    val downloadedKeys: Set<String> = emptySet(),
    val downloadingProgress: Map<String, Float> = emptyMap(),
    val totalStorageBytes: Long = 0L,
    val downloadedCountByReciter: Map<QuranReciter, Int> = emptyMap(),
    val isBulkDownloading: Boolean = false,
    val bulkDownloadReciter: QuranReciter? = null,
    val bulkDownloadCompleted: Int = 0,
    val bulkDownloadTotal: Int = 0,
    val lastErrorKey: String? = null
) {
    fun isDownloaded(reciter: QuranReciter, surahNumber: Int): Boolean =
        recitationKey(reciter, surahNumber) in downloadedKeys

    fun downloadProgress(reciter: QuranReciter, surahNumber: Int): Float? =
        downloadingProgress[recitationKey(reciter, surahNumber)]

    fun hasErrorFor(reciter: QuranReciter, surahNumber: Int): Boolean =
        lastErrorKey == recitationKey(reciter, surahNumber)

    val totalDownloadedCount: Int
        get() = downloadedKeys.size

    companion object {
        fun recitationKey(reciter: QuranReciter, surahNumber: Int): String =
            "${reciter.storageKey}_$surahNumber"
    }
}

/**
 * Manages saving, listing, and deleting Quran surah MP3 recitations in app-specific internal
 * storage (`filesDir/recitations`), requiring zero storage permissions and working fully offline.
 */
class RecitationDownloadManager(
    context: Context,
    private val scope: CoroutineScope
) {
    private val rootDir = File(context.applicationContext.filesDir, "recitations")
    private val activeJobs = ConcurrentHashMap<String, Job>()
    private var bulkJob: Job? = null

    private val _storageState = MutableStateFlow(RecitationStorageState())
    val storageState: StateFlow<RecitationStorageState> = _storageState.asStateFlow()

    init {
        scope.launch {
            refreshIndex()
        }
    }

    fun getLocalAudioFile(reciter: QuranReciter, surahNumber: Int): File? {
        val file = targetFile(reciter, surahNumber)
        return if (file.exists() && file.length() > MIN_VALID_AUDIO_BYTES) file else null
    }

    fun downloadSurah(reciter: QuranReciter, surahNumber: Int) {
        val key = RecitationStorageState.recitationKey(reciter, surahNumber)
        if (activeJobs[key]?.isActive == true) return
        if (getLocalAudioFile(reciter, surahNumber) != null) {
            scope.launch { refreshIndex() }
            return
        }

        val job = scope.launch {
            _storageState.update { state ->
                state.copy(
                    downloadingProgress = state.downloadingProgress + (key to 0f),
                    lastErrorKey = if (state.lastErrorKey == key) null else state.lastErrorKey
                )
            }
            val success = downloadSingleFile(reciter, surahNumber, key)
            _storageState.update { state ->
                state.copy(
                    downloadingProgress = state.downloadingProgress - key,
                    lastErrorKey = if (success) null else key
                )
            }
            refreshIndex()
            activeJobs.remove(key)
        }
        activeJobs[key] = job
    }

    fun cancelDownload(reciter: QuranReciter, surahNumber: Int) {
        val key = RecitationStorageState.recitationKey(reciter, surahNumber)
        activeJobs.remove(key)?.cancel()
        scope.launch(Dispatchers.IO) {
            tempFile(reciter, surahNumber).delete()
        }
        _storageState.update { state ->
            state.copy(downloadingProgress = state.downloadingProgress - key)
        }
    }

    fun deleteSurah(reciter: QuranReciter, surahNumber: Int) {
        cancelDownload(reciter, surahNumber)
        scope.launch(Dispatchers.IO) {
            targetFile(reciter, surahNumber).delete()
            tempFile(reciter, surahNumber).delete()
            refreshIndex()
        }
    }

    fun downloadAllForReciter(reciter: QuranReciter, surahNumbers: List<Int> = (67..114).toList()) {
        if (bulkJob?.isActive == true) return
        val remaining = surahNumbers.filter { getLocalAudioFile(reciter, it) == null }
        if (remaining.isEmpty()) {
            scope.launch { refreshIndex() }
            return
        }

        bulkJob = scope.launch {
            _storageState.update {
                it.copy(
                    isBulkDownloading = true,
                    bulkDownloadReciter = reciter,
                    bulkDownloadCompleted = 0,
                    bulkDownloadTotal = remaining.size,
                    lastErrorKey = null
                )
            }
            var completed = 0
            try {
                for (surahNum in remaining) {
                    currentCoroutineContext().ensureActive()
                    val key = RecitationStorageState.recitationKey(reciter, surahNum)
                    _storageState.update { state ->
                        state.copy(downloadingProgress = state.downloadingProgress + (key to 0f))
                    }
                    val ok = downloadSingleFile(reciter, surahNum, key)
                    _storageState.update { state ->
                        state.copy(
                            downloadingProgress = state.downloadingProgress - key,
                            lastErrorKey = if (ok) state.lastErrorKey else key
                        )
                    }
                    if (ok) {
                        completed++
                    }
                    _storageState.update {
                        it.copy(bulkDownloadCompleted = completed)
                    }
                    refreshIndex()
                }
            } finally {
                _storageState.update {
                    it.copy(
                        isBulkDownloading = false,
                        bulkDownloadReciter = null,
                        bulkDownloadCompleted = 0,
                        bulkDownloadTotal = 0
                    )
                }
                refreshIndex()
            }
        }
    }

    fun cancelBulkDownload() {
        bulkJob?.cancel()
        bulkJob = null
    }

    fun deleteAllRecitations() {
        cancelBulkDownload()
        activeJobs.values.forEach { it.cancel() }
        activeJobs.clear()
        scope.launch(Dispatchers.IO) {
            if (rootDir.exists()) {
                rootDir.deleteRecursively()
            }
            _storageState.update {
                it.copy(
                    downloadingProgress = emptyMap(),
                    lastErrorKey = null
                )
            }
            refreshIndex()
        }
    }

    private suspend fun downloadSingleFile(
        reciter: QuranReciter,
        surahNumber: Int,
        key: String
    ): Boolean = withContext(Dispatchers.IO) {
        val urls = listOf(reciter.primaryUrl(surahNumber), reciter.fallbackUrl(surahNumber))
        val destFile = targetFile(reciter, surahNumber)
        val partFile = tempFile(reciter, surahNumber)
        destFile.parentFile?.mkdirs()

        for (url in urls) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "WirdAlNoor-Android/1.0")
                }

                val code = connection.responseCode
                if (code !in 200..299) {
                    continue
                }
                val contentLength = connection.contentLengthLong
                partFile.delete()

                connection.inputStream.use { input ->
                    FileOutputStream(partFile).use { output ->
                        val buffer = ByteArray(16 * 1024)
                        var bytesReadTotal = 0L
                        var lastEmitTime = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count == -1) break
                            output.write(buffer, 0, count)
                            bytesReadTotal += count

                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= 150L) {
                                lastEmitTime = now
                                val progress = if (contentLength > 0L) {
                                    (bytesReadTotal.toFloat() / contentLength.toFloat()).coerceIn(0f, 0.99f)
                                } else {
                                    -1f
                                }
                                _storageState.update { state ->
                                    state.copy(downloadingProgress = state.downloadingProgress + (key to progress))
                                }
                            }
                        }
                        output.flush()
                    }
                }

                if (partFile.length() > MIN_VALID_AUDIO_BYTES) {
                    if (destFile.exists()) destFile.delete()
                    if (partFile.renameTo(destFile)) {
                        return@withContext true
                    }
                }
                partFile.delete()
            } catch (ce: CancellationException) {
                partFile.delete()
                throw ce
            } catch (_: Exception) {
                partFile.delete()
            } finally {
                connection?.disconnect()
            }
        }
        false
    }

    private suspend fun refreshIndex() = withContext(Dispatchers.IO) {
        val keys = HashSet<String>()
        val countsByReciter = HashMap<QuranReciter, Int>()
        var totalBytes = 0L

        for (reciter in QuranReciter.entries) {
            var reciterCount = 0
            val dir = File(rootDir, reciter.storageKey)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".mp3") && file.length() > MIN_VALID_AUDIO_BYTES) {
                        val surahNum = file.name.removeSuffix(".mp3").toIntOrNull()
                        if (surahNum != null) {
                            keys.add(RecitationStorageState.recitationKey(reciter, surahNum))
                            reciterCount++
                            totalBytes += file.length()
                        }
                    }
                }
            }
            countsByReciter[reciter] = reciterCount
        }

        _storageState.update {
            it.copy(
                downloadedKeys = keys,
                totalStorageBytes = totalBytes,
                downloadedCountByReciter = countsByReciter
            )
        }
    }

    private fun targetFile(reciter: QuranReciter, surahNumber: Int): File {
        val fileName = "${surahNumber.toString().padStart(3, '0')}.mp3"
        return File(File(rootDir, reciter.storageKey), fileName)
    }

    private fun tempFile(reciter: QuranReciter, surahNumber: Int): File {
        val fileName = "${surahNumber.toString().padStart(3, '0')}.mp3.part"
        return File(File(rootDir, reciter.storageKey), fileName)
    }

    companion object {
        private const val MIN_VALID_AUDIO_BYTES = 1024L
    }
}
