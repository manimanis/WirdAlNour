package com.example.data.remote

import com.example.data.local.entity.SurahAyahEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fetches verified Uthmani Arabic Quran text for a surah from official public Quran APIs,
 * with automatic fallback between api.quran.com and api.alquran.cloud.
 */
class QuranTextService {

    suspend fun fetchSurahAyahs(surahNumber: Int): Result<List<SurahAyahEntity>> =
        withContext(Dispatchers.IO) {
            val primary = runCatching { fetchFromQuranCom(surahNumber) }
            if (primary.isSuccess && primary.getOrNull()?.isNotEmpty() == true) {
                return@withContext primary
            }

            val fallback = runCatching { fetchFromAlQuranCloud(surahNumber) }
            if (fallback.isSuccess && fallback.getOrNull()?.isNotEmpty() == true) {
                return@withContext fallback
            }

            Result.failure(
                fallback.exceptionOrNull()
                    ?: primary.exceptionOrNull()
                    ?: IllegalStateException("Unable to load surah $surahNumber")
            )
        }

    private fun fetchFromQuranCom(surahNumber: Int): List<SurahAyahEntity> {
        val body = httpGet("https://api.quran.com/api/v4/quran/verses/uthmani?chapter_number=$surahNumber")
        val root = JSONObject(body)
        val versesArray = root.getJSONArray("verses")
        val result = ArrayList<SurahAyahEntity>(versesArray.length())

        for (i in 0 until versesArray.length()) {
            val item = versesArray.getJSONObject(i)
            val verseKey = item.optString("verse_key", "$surahNumber:${i + 1}")
            val ayahNumber = verseKey.substringAfter(':', "${i + 1}").toIntOrNull() ?: (i + 1)
            val text = item.getString("text_uthmani").trim()
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
        return result
    }

    private fun fetchFromAlQuranCloud(surahNumber: Int): List<SurahAyahEntity> {
        val body = httpGet("https://api.alquran.cloud/v1/surah/$surahNumber/quran-uthmani")
        val root = JSONObject(body)
        val data = root.getJSONObject("data")
        val ayahsArray = data.getJSONArray("ayahs")
        val result = ArrayList<SurahAyahEntity>(ayahsArray.length())

        for (i in 0 until ayahsArray.length()) {
            val item = ayahsArray.getJSONObject(i)
            val numberInSurah = item.getInt("numberInSurah")
            var text = item.getString("text").trim()
            if (numberInSurah == 1 && surahNumber != 1 && surahNumber != 9) {
                text = stripLeadingBasmalah(text)
            }
            if (text.isNotEmpty()) {
                result.add(
                    SurahAyahEntity(
                        surahNumber = surahNumber,
                        ayahNumber = numberInSurah,
                        arabicText = text
                    )
                )
            }
        }
        return result
    }

    private fun stripLeadingBasmalah(rawText: String): String {
        val cleaned = rawText.trimStart('\uFEFF').trim()
        val prefixes = listOf(
            "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
            "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        )
        for (prefix in prefixes) {
            if (cleaned.startsWith(prefix) && cleaned.length > prefix.length) {
                return cleaned.removePrefix(prefix).trim()
            }
        }
        // Fallback: if first 4 whitespace-separated words match Basmalah roots
        val words = cleaned.split(Regex("\\s+"))
        if (words.size > 4 && words[0].contains('ب') && words[3].contains('ح')) {
            val candidate = words.drop(4).joinToString(" ").trim()
            if (candidate.isNotEmpty() && stripDiacritics(words.take(4).joinToString(" ")) == "بسم الله الرحمن الرحيم") {
                return candidate
            }
        }
        return cleaned
    }

    private fun stripDiacritics(input: String): String {
        return input
            .replace(Regex("[\u064B-\u065F\u0670\u06D6-\u06ED]"), "")
            .replace('\u0671', 'ا')
            .trim()
    }

    private fun httpGet(urlString: String): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 12_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "WirdAlNoor-Android/1.0")
        }
        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("HTTP $code from $urlString")
            }
            BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } finally {
            connection.disconnect()
        }
    }
}
