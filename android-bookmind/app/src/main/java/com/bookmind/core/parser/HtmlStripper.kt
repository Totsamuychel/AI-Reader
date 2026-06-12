package com.bookmind.core.parser

import org.jsoup.Jsoup

/** = iOS `HTMLStripper`. Converts XHTML chapter markup to clean plain text. */
object HtmlStripper {
    fun plainText(html: String): String {
        if (html.isBlank()) return ""
        // Jsoup handles scripts/styles/entities; normalise whitespace afterwards.
        val text = Jsoup.parse(html).text()
        return text.replace(Regex("\\s+"), " ").trim()
    }
}
