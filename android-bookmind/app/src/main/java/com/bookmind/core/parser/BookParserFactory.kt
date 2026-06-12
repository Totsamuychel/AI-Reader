package com.bookmind.core.parser

import com.bookmind.core.model.BookFormat
import javax.inject.Inject

/** Resolves the right [BookParsing] for a book format. = iOS `parserFactory` closure. */
class BookParserFactory @Inject constructor(
    private val epubParser: EpubParser,
    private val txtParser: TxtParser
) {
    fun parser(format: BookFormat): BookParsing = when (format) {
        BookFormat.EPUB -> epubParser
        BookFormat.TXT -> txtParser
    }
}
