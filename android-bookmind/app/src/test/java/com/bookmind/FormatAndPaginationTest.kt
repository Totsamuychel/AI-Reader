package com.bookmind

import com.bookmind.core.model.BookFormat
import com.bookmind.ui.reader.paginate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatDetectionTest {

    @Test
    fun `extensions map to formats`() {
        assertEquals(BookFormat.EPUB, BookFormat.fromExtension("epub"))
        assertEquals(BookFormat.EPUB, BookFormat.fromExtension("EPUB"))
        assertEquals(BookFormat.FB2, BookFormat.fromExtension("fb2"))
        assertEquals(BookFormat.PDF, BookFormat.fromExtension("pdf"))
        assertEquals(BookFormat.TXT, BookFormat.fromExtension("txt"))
    }

    @Test
    fun `unknown extension falls back to txt`() {
        assertEquals(BookFormat.TXT, BookFormat.fromExtension("rtf"))
        assertEquals(BookFormat.TXT, BookFormat.fromExtension(""))
    }
}

class PaginationTest {

    @Test
    fun `blank text yields a single empty page`() {
        assertEquals(listOf(""), paginate("", 100))
    }

    @Test
    fun `short text stays on one page`() {
        assertEquals(1, paginate("A short paragraph.", 100).size)
    }

    @Test
    fun `long text splits into multiple pages without dropping words`() {
        val para = (1..200).joinToString(" ") { "word$it" }
        val pages = paginate(para, 120)
        assertTrue("expected multiple pages", pages.size > 1)
        // No word is split across pages: every page recombines into the source words.
        val recombined = pages.joinToString(" ").split(Regex("\\s+")).filter { it.isNotBlank() }
        assertEquals(200, recombined.size)
    }

    @Test
    fun `paragraph boundaries are respected when they fit`() {
        val text = "First para.\n\nSecond para.\n\nThird para."
        val pages = paginate(text, 1000)
        assertEquals(1, pages.size)
        assertTrue(pages[0].contains("First para."))
        assertTrue(pages[0].contains("Third para."))
    }
}
