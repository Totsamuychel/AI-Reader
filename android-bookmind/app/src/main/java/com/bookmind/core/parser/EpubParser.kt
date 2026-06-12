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
import org.jsoup.parser.Parser
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

/**
 * EPUB parser. = iOS `EPUBParser`.
 *
 * EPUB is a ZIP container. We read `META-INF/container.xml` to locate the OPF,
 * parse the OPF manifest + spine, and expose each spine item as a [Chapter].
 * Reading the archive directly (via [ZipInputStream]) avoids a heavyweight
 * third-party EPUB dependency.
 */
class EpubParser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParsing {

    private data class ManifestItem(val id: String, val href: String, val mediaType: String)

    private data class Opf(
        val opfDir: String,
        val manifest: Map<String, ManifestItem>,
        val spine: List<String>
    )

    override suspend fun parseChapters(book: Book): List<Chapter> = withContext(Dispatchers.IO) {
        val entries = readAllEntries(book.fileUri)
        val opf = parseOpf(entries)
        opf.spine.mapIndexedNotNull { idx, idref ->
            val item = opf.manifest[idref] ?: return@mapIndexedNotNull null
            Chapter(
                id = ChapterID("${book.id.raw}#$idx"),
                bookID = book.id,
                index = idx,
                title = item.href.substringAfterLast('/'),
                contentRef = normalizePath(opf.opfDir, item.href)
            )
        }
    }

    override suspend fun rawText(chapter: Chapter, book: Book): String = withContext(Dispatchers.IO) {
        val ref = chapter.contentRef ?: return@withContext ""
        val entries = readAllEntries(book.fileUri)
        val html = entries[ref]?.toString(Charsets.UTF_8) ?: return@withContext ""
        HtmlStripper.plainText(html)
    }

    // MARK: - ZIP reading

    private fun readAllEntries(uri: String): Map<String, ByteArray> {
        val result = HashMap<String, ByteArray>()
        val input = context.contentResolver.openInputStream(Uri.parse(uri))
            ?: throw ReaderError.FileNotReadable(uri)
        input.use { stream ->
            ZipInputStream(stream).use { zip ->
                var entry = zip.nextEntry
                val buffer = ByteArray(16 * 1024)
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val out = ByteArrayOutputStream()
                        var read = zip.read(buffer)
                        while (read != -1) {
                            out.write(buffer, 0, read)
                            read = zip.read(buffer)
                        }
                        result[entry.name] = out.toByteArray()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        }
        return result
    }

    // MARK: - OPF parsing

    private fun parseOpf(entries: Map<String, ByteArray>): Opf {
        val containerXml = entries["META-INF/container.xml"]?.toString(Charsets.UTF_8)
            ?: throw ReaderError.ParsingFailed("container.xml not found")
        val opfPath = Regex("""full-path\s*=\s*"([^"]+)"""")
            .find(containerXml)?.groupValues?.get(1)
            ?: throw ReaderError.ParsingFailed("OPF path not found in container.xml")

        val opfXml = entries[opfPath]?.toString(Charsets.UTF_8)
            ?: throw ReaderError.ParsingFailed("OPF not found at $opfPath")
        val doc = Jsoup.parse(opfXml, "", Parser.xmlParser())

        val manifest = doc.select("manifest > item").mapNotNull { el ->
            val id = el.attr("id").ifEmpty { return@mapNotNull null }
            val href = el.attr("href").ifEmpty { return@mapNotNull null }
            val media = el.attr("media-type").ifEmpty { "application/xhtml+xml" }
            ManifestItem(id, href, media)
        }.associateBy { it.id }

        val spine = doc.select("spine > itemref").mapNotNull {
            it.attr("idref").ifEmpty { null }
        }

        val opfDir = opfPath.substringBeforeLast('/', missingDelimiterValue = "")
        return Opf(opfDir, manifest, spine)
    }

    /** Resolves an OPF-relative href against the OPF directory into a zip entry path. */
    private fun normalizePath(opfDir: String, href: String): String {
        val combined = if (opfDir.isEmpty()) href else "$opfDir/$href"
        val parts = ArrayDeque<String>()
        for (segment in combined.split('/')) {
            when (segment) {
                "", "." -> {}
                ".." -> if (parts.isNotEmpty()) parts.removeLast()
                else -> parts.addLast(segment)
            }
        }
        return parts.joinToString("/")
    }
}
