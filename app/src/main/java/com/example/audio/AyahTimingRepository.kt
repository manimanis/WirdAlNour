package com.example.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AyahTiming(
    val ayahNumber: Int, // 0 = Basmalah, 1..N = verse number
    val startMs: Long,
    val endMs: Long
)

/**
 * Loads bundled millisecond-accurate ayah timing tables from `assets/quran_timings_67_114.json`
 * so audio-to-text synchronization works 100% offline for all 4 reciters and all 48 surahs.
 */
class AyahTimingRepository(
    context: Context
) {
    private val appContext = context.applicationContext
    private val loadMutex = Mutex()

    @Volatile
    private var cachedTimings: Map<String, Map<Int, List<AyahTiming>>>? = null

    suspend fun getTimings(reciter: QuranReciter, surahNumber: Int): List<AyahTiming> {
        val all = ensureLoaded()
        return all[reciter.storageKey]?.get(surahNumber).orEmpty()
    }

    fun findActiveAyah(timings: List<AyahTiming>, positionMs: Long): Int? {
        if (timings.isEmpty() || positionMs < 0L) return null

        val firstVerse = timings.firstOrNull { it.ayahNumber >= 1 }
        if (firstVerse != null && positionMs < firstVerse.startMs) {
            // Sheikh is reciting the Basmalah before verse 1
            return 0
        }

        // Exact interval match
        for (entry in timings) {
            if (positionMs >= entry.startMs && positionMs < entry.endMs) {
                return entry.ayahNumber
            }
        }

        // If position sits in a brief silent gap between verses or at the very end
        var lastPassed: AyahTiming? = null
        for (entry in timings) {
            if (entry.startMs <= positionMs) {
                lastPassed = entry
            } else {
                break
            }
        }
        return lastPassed?.ayahNumber
    }

    fun findStartMsForAyah(timings: List<AyahTiming>, ayahNumber: Int): Long? {
        return timings.firstOrNull { it.ayahNumber == ayahNumber }?.startMs
    }

    private suspend fun ensureLoaded(): Map<String, Map<Int, List<AyahTiming>>> {
        cachedTimings?.let { return it }
        return loadMutex.withLock {
            cachedTimings?.let { return@withLock it }
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = appContext.assets.open(ASSET_FILE_NAME)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val root = JSONObject(raw)
                    val reciterMap = HashMap<String, Map<Int, List<AyahTiming>>>()
                    val reciterKeys = root.keys()
                    while (reciterKeys.hasNext()) {
                        val reciterKey = reciterKeys.next()
                        val surahsObj = root.getJSONObject(reciterKey)
                        val surahMap = HashMap<Int, List<AyahTiming>>()
                        val surahKeys = surahsObj.keys()
                        while (surahKeys.hasNext()) {
                            val surahKey = surahKeys.next()
                            val surahNum = surahKey.toIntOrNull() ?: continue
                            val arr = surahsObj.getJSONArray(surahKey)
                            val list = ArrayList<AyahTiming>(arr.length())
                            for (i in 0 until arr.length()) {
                                val tuple = arr.getJSONArray(i)
                                list.add(
                                    AyahTiming(
                                        ayahNumber = tuple.getInt(0),
                                        startMs = tuple.getLong(1),
                                        endMs = tuple.getLong(2)
                                    )
                                )
                            }
                            surahMap[surahNum] = list
                        }
                        reciterMap[reciterKey] = surahMap
                    }
                    reciterMap
                }.getOrDefault(emptyMap())
            }
            cachedTimings = parsed
            parsed
        }
    }

    companion object {
        const val ASSET_FILE_NAME = "quran_timings_67_114.json"
    }
}
