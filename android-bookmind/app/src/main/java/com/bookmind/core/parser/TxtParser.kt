package com.bookmind.core.parser

import android.content.Context
import android.net.Uri
import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ChapterID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset
import javax.inject.Inject

/** = iOS `TXTParser`. Heuristically splits plain text into chapters. */
class TxtParser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParsing {

    data class ChapterSlice(val title: String?, val body: String)

    override suspend fun parseChapters(book: Book): List<Chapter> = withContext(Dispatchers.IO) {
        val text = readText(book.fileUri)
        val splits = splitChapters(text)

        if (splits.isEmpty()) {
            // Single-chapter fallback.
            return@withContext listOf(
                Chapter(
                    id = ChapterID("${book.id.raw}#0"),
                    bookID = book.id,
                    index = 0,
                    title = book.title,
                    contentRef = book.fileUri
                )
            )
        }

        splits.mapIndexed { idx, slice ->
            Chapter(
                id = ChapterID("${book.id.raw}#$idx"),
                bookID = book.id,
                index = idx,
                title = slice.title,
                contentRef = book.fileUri
            )
        }
    }

    override suspend fun rawText(chapter: Chapter, book: Book): String = withContext(Dispatchers.IO) {
        val full = readText(book.fileUri)
        val splits = splitChapters(full)
        if (splits.isEmpty()) return@withContext full
        if (chapter.index !in splits.indices) return@withContext ""
        splits[chapter.index].body
    }

    // MARK: - Internal

    fun splitChapters(text: String): List<ChapterSlice> {
        val matches = chapterMarkers
            .flatMap { regex -> regex.findAll(text).map { it.range.first to it.value.trim() } }
            .sortedBy { it.first }

        if (matches.isEmpty()) return emptyList()

        val slices = mutableListOf<ChapterSlice>()
        for (i in matches.indices) {
            val (start, title) = matches[i]
            val bodyStart = start + title.length
            val bodyEnd = if (i + 1 < matches.size) matches[i + 1].first else text.length
            if (bodyEnd <= bodyStart) continue
            val body = text.substring(bodyStart, bodyEnd).trim()
            slices.add(ChapterSlice(title, body))
        }
        return slices
    }

    private fun readText(uri: String): String {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            ?: throw ReaderError.FileNotReadable(uri)
        val encodings = listOf(Charsets.UTF_8, Charsets.UTF_16, Charsets.ISO_8859_1, windows1251)
        for (enc in encodings) {
            runCatching { decodeStrict(bytes, enc) }.getOrNull()?.let { return it }
        }
        throw ReaderError.ParsingFailed("Could not decode text file")
    }

    private fun decodeStrict(bytes: ByteArray, charset: Charset): String {
        val decoder = charset.newDecoder() // throws on malformed input
        return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }

    companion object {
        private val windows1251: Charset = Charset.forName("windows-1251")

        private val chapterMarkers: List<Regex> = listOf(
            Regex("""^\s*Chapter\s+\d+.*$""", RegexOption.MULTILINE),
            Regex("""^\s*CHAPTER\s+[IVXLC\d]+.*$""", RegexOption.MULTILINE),
            Regex("""^\s*Глава\s+\d+.*$""", RegexOption.MULTILINE),
            Regex("""^\s*ГЛАВА\s+[IVXLC\d]+.*$""", RegexOption.MULTILINE),
            Regex("""^\s*\d+\.\s+\p{Lu}.*$""", RegexOption.MULTILINE)
        )
    }
}
