package com.mangaru.app.translator

import com.google.gson.JsonParser
import com.mangaru.app.cache.TranslationCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class GoogleTranslatorEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun translate(text: String, sourceLang: String = "auto", targetLang: String = "ru"): String = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext ""

        TranslationCache.get(text)?.let { cached ->
            return@withContext cached
        }

        try {
            val encodedText = URLEncoder.encode(text, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=$sourceLang&tl=$targetLang&dt=t&q=$encodedText"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext text

            val jsonArray = JsonParser.parseString(body).asJsonArray
            val sentencesArray = jsonArray.get(0).asJsonArray

            val sb = StringBuilder()
            for (i in 0 until sentencesArray.size()) {
                val sentence = sentencesArray.get(i).asJsonArray
                sb.append(sentence.get(0).asString)
            }

            val result = sb.toString()
            if (result.isNotBlank()) {
                TranslationCache.put(text, result)
                return@withContext result
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return@withContext text
    }
}
