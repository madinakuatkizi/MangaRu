package com.mangaru.app.cache

import java.util.concurrent.ConcurrentHashMap

object TranslationCache {

    private val cache = ConcurrentHashMap<String, String>()

    fun get(originalText: String): String? {
        val clean = cleanText(originalText)
        return cache[clean]
    }

    fun put(originalText: String, translatedText: String) {
        val clean = cleanText(originalText)
        if (clean.isNotEmpty()) {
            cache[clean] = translatedText
        }
    }

    fun clear() {
        cache.clear()
    }

    private fun cleanText(text: String): String {
        return text.trim().lowercase().replace("\\s+".toRegex(), " ")
    }
}
