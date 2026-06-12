package com.bookmind.core.parser

import com.bookmind.core.model.Book
import com.bookmind.core.model.Chapter

/** = iOS `BookParsing`. Splits a book into chapters and extracts plain text. */
interface BookParsing {
    suspend fun parseChapters(book: Book): List<Chapter>
    suspend fun rawText(chapter: Chapter, book: Book): String
}

sealed class ReaderError(message: String) : Exception(message) {
    class UnsupportedFormat(ext: String) : ReaderError("Unsupported format: $ext")
    class FileNotReadable(path: String) : ReaderError("File not readable: $path")
    class ParsingFailed(reason: String) : ReaderError("Parsing failed: $reason")
    object BookNotOpen : ReaderError("No book is currently open")
}
