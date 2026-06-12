package com.bookmind.retrieval

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/** A short factual snippet fetched from the web for LLM context. */
data class WebSnippet(
    val title: String,
    val text: String,
    val sourceUrl: String
)

/** Optional internet tool the assistant can use for out-of-book questions. */
interface WebSearching {
    suspend fun search(query: String): WebSnippet?
}

/**
 * Key-less web lookup: DuckDuckGo Instant Answer first, Wikipedia REST summary
 * as a fallback (ru wiki for Cyrillic queries, en otherwise).
 */
@Singleton
class DuckDuckGoWikipediaSearch @Inject constructor() : WebSearching {

    override suspend fun search(query: String): WebSnippet? = withContext(Dispatchers.IO) {
        val trimmed = query.trim().take(200)
        if (trimmed.isEmpty()) return@withContext null
        duckDuckGo(trimmed) ?: wikipedia(trimmed)
    }

    private fun duckDuckGo(query: String): WebSnippet? = runCatching {
        val q = URLEncoder.encode(query, "UTF-8")
        val json = fetch("https://api.duckduckgo.com/?q=$q&format=json&no_html=1&skip_disambig=1")
        parseDuckDuckGo(json)
    }.getOrNull()

    private fun wikipedia(query: String): WebSnippet? = runCatching {
        val lang = if (query.any { it in 'Ѐ'..'ӿ' }) "ru" else "en"
        val title = URLEncoder.encode(query.replace(' ', '_'), "UTF-8")
        val json = fetch("https://$lang.wikipedia.org/api/rest_v1/page/summary/$title")
        parseWikipediaSummary(json)
    }.getOrNull()

    private fun fetch(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "BookMind/1.0 (reading assistant)")
        }
        try {
            if (connection.responseCode !in 200..299) {
                throw IOException("HTTP ${connection.responseCode}")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val MAX_SNIPPET_CHARS = 900

        fun parseDuckDuckGo(json: String): WebSnippet? {
            val obj = JSONObject(json)
            val text = obj.optString("AbstractText").ifBlank { obj.optString("Answer") }
            if (text.isBlank()) return null
            return WebSnippet(
                title = obj.optString("Heading").ifBlank { "DuckDuckGo" },
                text = text.take(MAX_SNIPPET_CHARS),
                sourceUrl = obj.optString("AbstractURL").ifBlank { "https://duckduckgo.com" }
            )
        }

        fun parseWikipediaSummary(json: String): WebSnippet? {
            val obj = JSONObject(json)
            val extract = obj.optString("extract")
            if (extract.isBlank()) return null
            return WebSnippet(
                title = obj.optString("title").ifBlank { "Wikipedia" },
                text = extract.take(MAX_SNIPPET_CHARS),
                sourceUrl = obj.optJSONObject("content_urls")
                    ?.optJSONObject("desktop")?.optString("page")
                    .orEmpty()
                    .ifBlank { "https://wikipedia.org" }
            )
        }
    }
}
