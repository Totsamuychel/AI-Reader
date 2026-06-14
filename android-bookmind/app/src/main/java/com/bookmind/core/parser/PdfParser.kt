package com.bookmind.core.parser

import android.content.Context
import android.net.Uri
import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter
import com.bookmind.core.model.ChapterID
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * PDF parser backed by PDFBox-Android. Each page becomes a [Chapter] so the
 * reader's chapter navigation maps onto page navigation. Text is extracted with
 * [PDFTextStripper]; scanned/image-only PDFs yield empty text (no OCR).
 */
class PdfParser @Inject constructor(
    @ApplicationContext private val context: Context
) : BookParsing {

    init {
        // Loads PDFBox's bundled fonts/resources from assets; idempotent.
        PDFBoxResourceLoader.init(context.applicationContext)
    }

    override suspend fun parseChapters(book: Book): List<Chapter> = withContext(Dispatchers.IO) {
        withDocument(book.fileUri) { doc ->
            val pages = doc.numberOfPages.coerceAtLeast(1)
            (0 until pages).map { idx ->
                Chapter(
                    id = ChapterID("${book.id.raw}#$idx"),
                    bookID = book.id,
                    index = idx,
                    title = "Page ${idx + 1}",
                    contentRef = book.fileUri
                )
            }
        }
    }

    override suspend fun rawText(chapter: Chapter, book: Book): String = withContext(Dispatchers.IO) {
        withDocument(book.fileUri) { doc ->
            val pageNumber = chapter.index + 1 // PDFTextStripper is 1-based
            if (pageNumber < 1 || pageNumber > doc.numberOfPages) return@withDocument ""
            val stripper = PDFTextStripper().apply {
                startPage = pageNumber
                endPage = pageNumber
            }
            stripper.getText(doc).replace(Regex("[ \\t]+"), " ").trim()
        }
    }

    private fun <T> withDocument(uri: String, block: (PDDocument) -> T): T {
        val input = context.contentResolver.openInputStream(Uri.parse(uri))
            ?: throw ReaderError.FileNotReadable(uri)
        return input.use { stream ->
            runCatching { PDDocument.load(stream) }.getOrElse {
                throw ReaderError.ParsingFailed("PDF could not be opened: ${it.message}")
            }.use { doc -> block(doc) }
        }
    }
}
