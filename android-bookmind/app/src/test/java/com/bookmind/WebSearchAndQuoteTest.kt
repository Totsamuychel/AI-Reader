package com.bookmind

import com.bookmind.core.model.BookID
import com.bookmind.core.model.ChapterID
import com.bookmind.core.model.UserQuote
import com.bookmind.persistence.toDomain
import com.bookmind.persistence.toEntity
import com.bookmind.retrieval.DuckDuckGoWikipediaSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebSearchParsingTest {

    @Test
    fun `duckduckgo abstract is parsed into a snippet`() {
        val json = """
            {
              "Heading": "Leo Tolstoy",
              "AbstractText": "Count Lev Nikolayevich Tolstoy was a Russian writer.",
              "AbstractURL": "https://en.wikipedia.org/wiki/Leo_Tolstoy"
            }
        """.trimIndent()
        val snippet = DuckDuckGoWikipediaSearch.parseDuckDuckGo(json)!!
        assertEquals("Leo Tolstoy", snippet.title)
        assertEquals("https://en.wikipedia.org/wiki/Leo_Tolstoy", snippet.sourceUrl)
        assertTrue(snippet.text.contains("Russian writer"))
    }

    @Test
    fun `empty duckduckgo answer yields null`() {
        assertNull(DuckDuckGoWikipediaSearch.parseDuckDuckGo("""{"AbstractText":""}"""))
    }

    @Test
    fun `wikipedia summary is parsed and truncated`() {
        val longExtract = "a".repeat(2000)
        val json = """
            {
              "title": "War and Peace",
              "extract": "$longExtract",
              "content_urls": {"desktop": {"page": "https://en.wikipedia.org/wiki/War_and_Peace"}}
            }
        """.trimIndent()
        val snippet = DuckDuckGoWikipediaSearch.parseWikipediaSummary(json)!!
        assertEquals("War and Peace", snippet.title)
        assertEquals(DuckDuckGoWikipediaSearch.MAX_SNIPPET_CHARS, snippet.text.length)
        assertEquals("https://en.wikipedia.org/wiki/War_and_Peace", snippet.sourceUrl)
    }
}

class UserQuoteMapperTest {

    @Test
    fun `quote survives an entity round-trip`() {
        val quote = UserQuote(
            id = "q-1",
            bookID = BookID("book-1"),
            chapterID = ChapterID("ch-3"),
            chapterIndex = 2,
            text = "It was the best of times.",
            note = "opening",
            createdAt = 1718000000000L
        )
        assertEquals(quote, quote.toEntity().toDomain())
    }
}
