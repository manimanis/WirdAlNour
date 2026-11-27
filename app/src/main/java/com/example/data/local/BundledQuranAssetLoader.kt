package com.example.data.local

import android.content.Context
import com.example.data.local.entity.SurahAyahEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Reads the bundled Uthmani Arabic Quran text for Surahs 67 (Al-Mulk) through 114 (An-Nas)
 * from app assets so every surah is guaranteed to be available 100% offline from first launch.
 */
class BundledQuranAssetLoader(
    context: Context
) {
    private val appContext = context.applicationContext

    suspend fun loadAllSurahAyahs(): List<SurahAyahEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = appContext.assets.open(ASSET_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val root = JSONObject(jsonText)
            val result = ArrayList<SurahAyahEntity>(EXPECTED_TOTAL_AYAHS)
            val keys = root.keys()
            while (keys.hasNext()) {
                val surahKey = keys.next()
                val surahNum = surahKey.toIntOrNull() ?: continue
                val versesArray = root.getJSONArray(surahKey)
                for (i in 0 until versesArray.length()) {
                    val item = versesArray.getJSONObject(i)
                    val ayahNumber = item.getInt("n")
                    val text = item.getString("t").trim()
                    if (text.isNotEmpty()) {
                        result.add(
                            SurahAyahEntity(
                                surahNumber = surahNum,
                                ayahNumber = ayahNumber,
                                arabicText = text
                            )
                        )
                    }
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    suspend fun loadSurahAyahs(surahNumber: Int): List<SurahAyahEntity> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = appContext.assets.open(ASSET_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
            val root = JSONObject(jsonText)
            val versesArray = root.optJSONArray(surahNumber.toString()) ?: return@runCatching emptyList()
            val result = ArrayList<SurahAyahEntity>(versesArray.length())
            for (i in 0 until versesArray.length()) {
                val item = versesArray.getJSONObject(i)
                val ayahNumber = item.getInt("n")
                val text = item.getString("t").trim()
                if (text.isNotEmpty()) {
                    result.add(
                        SurahAyahEntity(
                            surahNumber = surahNumber,
                            ayahNumber = ayahNumber,
                            arabicText = text
                        )
                    )
                }
            }
            result
        }.getOrDefault(emptyList())
    }

    companion object {
        const val ASSET_FILE_NAME = "quran_surahs_67_114.json"
        const val EXPECTED_TOTAL_AYAHS = 995
    }
}
