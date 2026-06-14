package com.bookmind.sync

import android.content.Context
import android.net.Uri
import com.bookmind.core.model.BookID
import com.bookmind.core.model.ChapterID
import com.bookmind.core.model.HighlightColor
import com.bookmind.core.model.ReadingPosition
import com.bookmind.core.model.UserQuote
import com.bookmind.persistence.QuoteStoring
import com.bookmind.persistence.ReadingProgressStoring
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * JSON backup of reading progress + highlights, written/read through a
 * Storage-Access-Framework [Uri]. Books themselves aren't backed up (the source
 * files live outside the app), only the user's reading state.
 */
class SyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressStore: ReadingProgressStoring,
    private val quoteStore: QuoteStoring
) {
    data class Result(val progressCount: Int, val quoteCount: Int)

    suspend fun export(uri: Uri): Result = withContext(Dispatchers.IO) {
        val positions = progressStore.allPositions()
        val quotes = quoteStore.allQuotes()
        val json = JSONObject().apply {
            put("version", FORMAT_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("progress", JSONArray().apply { positions.forEach { put(it.toJson()) } })
            put("quotes", JSONArray().apply { quotes.forEach { put(it.toJson()) } })
        }
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.toString(2).toByteArray(Charsets.UTF_8))
        } ?: error("Could not open backup file for writing")
        Result(positions.size, quotes.size)
    }

    suspend fun import(uri: Uri): Result = withContext(Dispatchers.IO) {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open backup file")
        val json = JSONObject(String(bytes, Charsets.UTF_8))

        val progressArr = json.optJSONArray("progress") ?: JSONArray()
        var progressCount = 0
        for (i in 0 until progressArr.length()) {
            progressStore.savePosition(progressArr.getJSONObject(i).toPosition())
            progressCount++
        }

        val quotesArr = json.optJSONArray("quotes") ?: JSONArray()
        var quoteCount = 0
        for (i in 0 until quotesArr.length()) {
            quoteStore.save(quotesArr.getJSONObject(i).toQuote())
            quoteCount++
        }
        Result(progressCount, quoteCount)
    }

    private fun ReadingPosition.toJson() = JSONObject().apply {
        put("bookId", bookID.raw)
        put("chapterId", chapterID?.raw)
        put("progressFraction", progressFraction)
        put("updatedAt", updatedAt)
    }

    private fun JSONObject.toPosition() = ReadingPosition(
        bookID = BookID(getString("bookId")),
        chapterID = optString("chapterId").takeIf { it.isNotBlank() && it != "null" }?.let { ChapterID(it) },
        progressFraction = optDouble("progressFraction", 0.0),
        updatedAt = optLong("updatedAt", System.currentTimeMillis())
    )

    private fun UserQuote.toJson() = JSONObject().apply {
        put("id", id)
        put("bookId", bookID.raw)
        put("chapterId", chapterID?.raw)
        put("chapterIndex", chapterIndex)
        put("text", text)
        put("note", note)
        put("color", color.name)
        put("createdAt", createdAt)
    }

    private fun JSONObject.toQuote() = UserQuote(
        id = getString("id"),
        bookID = BookID(getString("bookId")),
        chapterID = optString("chapterId").takeIf { it.isNotBlank() && it != "null" }?.let { ChapterID(it) },
        chapterIndex = optInt("chapterIndex", 0),
        text = getString("text"),
        note = optString("note").takeIf { it.isNotBlank() && it != "null" },
        color = HighlightColor.fromName(optString("color")),
        createdAt = optLong("createdAt", System.currentTimeMillis())
    )

    companion object {
        const val FORMAT_VERSION = 1
    }
}
