package com.bookmind.core.parser

import android.content.Context
import android.net.Uri
import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ChapterID
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.nio.charset.Charset
import javax.inject.Inject

/**
 * FB2 (FictionBook) parser. FB2 is a single XML file whose `<body>` holds nested
 * `<section>` elements — we treat each top-level section as a chapter. Like the
 * TXT parser the source file is re-read on demand rather than cached.
 */
class Fb2Parser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParsing {

    override suspend fun parseChapters(book: Book): List<Chapter> = withContext(Dispatchers.IO) {
        val sections = topLevelSections(book.fileUri)
        if (sections.isEmpty()) {
            return@withContext listOf(
                Chapter(ChapterID("${book.id.raw}#0"), book.id, 0, book.title, book.fileUri)
            )
        }
        sections.mapIndexed { idx, section ->
            Chapter(
                id = ChapterID("${book.id.raw}#$idx"),
                bookID = book.id,
                index = idx,
                title = sectionTitle(section) ?: "Section ${idx + 1}",
                contentRef = book.fileUri
            )
        }
    }

    override suspend fun rawText(chapter: Chapter, book: Book): String = withContext(Dispatchers.IO) {
        val sections = topLevelSections(book.fileUri)
        if (sections.isEmpty()) return@withContext bodyText(book.fileUri)
        val section = sections.getOrNull(chapter.index) ?: return@withContext ""
        sectionText(section)
    }

    // MARK: - Internal

    private fun parseDocument(uri: String): org.jsoup.nodes.Document {
        val bytes = context.contentResolver.openInputStream(Uri.parse(uri))?.use { it.readBytes() }
            ?: throw ReaderError.FileNotReadable(uri)
        val xml = decode(bytes)
        return Jsoup.parse(xml, "", Parser.xmlParser())
    }

    private fun topLevelSections(uri: String): List<Element> {
        val doc = parseDocument(uri)
        // FB2 may have multiple <body> elements (main + notes); use the first.
        val body = doc.select("body").firstOrNull() ?: return emptyList()
        return body.children().toList().filter { it.tagName().equals("section", ignoreCase = true) }
    }

    private fun sectionTitle(section: Element): String? {
        val title = section.selectFirst("title") ?: return null
        return title.text().trim().ifBlank { null }
    }

    private fun sectionText(section: Element): String {
        // Paragraphs (and verse lines) in reading order, blank line between blocks.
        val blocks = section.select("p, v, subtitle")
        if (blocks.isEmpty()) return section.text().replace(Regex("\\s+"), " ").trim()
        return blocks.joinToString("\n\n") { it.text().trim() }.trim()
    }

    private fun bodyText(uri: String): String {
        val doc = parseDocument(uri)
        val body = doc.select("body").firstOrNull() ?: return ""
        return body.select("p, v, subtitle").joinToString("\n\n") { it.text().trim() }.trim()
    }

    /** FB2 declares its encoding in the XML prolog; try UTF-8 then windows-1251. */
    private fun decode(bytes: ByteArray): String {
        val head = String(bytes, 0, minOf(bytes.size, 200), Charsets.ISO_8859_1).lowercase()
        val declared = Regex("""encoding\s*=\s*["']([^"']+)["']""").find(head)?.groupValues?.get(1)
        val charset = declared?.let { runCatching { Charset.forName(it) }.getOrNull() }
        if (charset != null) return String(bytes, charset)
        return runCatching { strictUtf8(bytes) }.getOrNull()
            ?: String(bytes, Charset.forName("windows-1251"))
    }

    private fun strictUtf8(bytes: ByteArray): String {
        val decoder = Charsets.UTF_8.newDecoder()
        return decoder.decode(java.nio.ByteBuffer.wrap(bytes)).toString()
    }
}
