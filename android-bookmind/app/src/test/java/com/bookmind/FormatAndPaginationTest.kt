package com.bookmind

import com.bookmind.core.model.BookFormat
import com.bookmind.ui.reader.normalizeChapterText
import com.bookmind.ui.reader.paginate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun `pages are not duplicated`() {
        // A run of distinct paragraphs must produce distinct, non-repeating pages.
        val text = (1..60).joinToString("\n\n") { "Paragraph number $it with some filler words." }
        val pages = paginate(text, 120)
        assertTrue("expected multiple pages", pages.size > 1)
        assertEquals("pages should be unique", pages.size, pages.distinct().size)
        // No adjacent page repeats either.
        for (i in 1 until pages.size) assertFalse(pages[i] == pages[i - 1])
    }
}

class NormalizeTest {

    @Test
    fun `blank input yields empty string`() {
        assertEquals("", normalizeChapterText("   \n\n  "))
    }

    @Test
    fun `hard-wrapped lines are joined within a paragraph`() {
        val raw = "This is a sentence\nbroken across\nthree lines."
        assertEquals("This is a sentence broken across three lines.", normalizeChapterText(raw))
    }

    @Test
    fun `paragraph breaks are preserved as a single blank line`() {
        val raw = "First.\n\n\n\nSecond."
        assertEquals("First.\n\nSecond.", normalizeChapterText(raw))
    }

    @Test
    fun `repeated spaces and stray whitespace collapse`() {
        val raw = "  Lots    of     space   here.  "
        assertEquals("Lots of space here.", normalizeChapterText(raw))
    }
}
