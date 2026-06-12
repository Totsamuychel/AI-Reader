package com.bookmind.memory

import java.text.BreakIterator

/** Locale-aware sentence splitting — replaces iOS `NSLinguisticTagger`. */
internal object Sentences {
    fun split(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val iterator = BreakIterator.getSentenceInstance()
        iterator.setText(text)
        val result = mutableListOf<String>()
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val s = text.substring(start, end).trim()
            if (s.isNotEmpty()) result.add(s)
            start = end
            end = iterator.next()
        }
        if (result.isEmpty()) {
            return text.split(".")
                .filter { it.isNotBlank() }
                .map { it.trim() + "." }
        }
        return result
    }

    fun leading(text: String, count: Int): List<String> =
        split(text).take(count)
}
