package com.bookmind.memory

import com.bookmind.core.model.Chapter
import com.bookmind.core.model.Character
import javax.inject.Inject

/**
 * = iOS `HeuristicCharacterDetector`.
 * Detects characters by extracting capitalized, repeated name-like tokens.
 */
class HeuristicCharacterDetector @Inject constructor() : CharacterDetecting {

    override fun detectCharacters(
        chapter: Chapter,
        rawText: String,
        existing: List<Character>
    ): List<Character> {
        val names = extractCandidateNames(rawText)
        val byCanonical = HashMap<String, Character>()
        for (c in existing) byCanonical[c.canonicalName.lowercase()] = c

        val result = mutableListOf<Character>()
        for (name in names) {
            val key = name.lowercase()
            val prior = byCanonical[key]
            if (prior != null) {
                if (chapter.index > prior.lastSafeChapterIndex) {
                    val updated = prior.copy(lastSafeChapterIndex = chapter.index)
                    byCanonical[key] = updated
                    result.add(updated)
                }
            } else {
                val new = Character(
                    id = "${chapter.bookID.raw}#char-${stableSlug(name)}",
                    bookID = chapter.bookID,
                    canonicalName = name,
                    aliases = emptyList(),
                    firstChapterIndex = chapter.index,
                    lastSafeChapterIndex = chapter.index,
                    safeSummary = null
                )
                byCanonical[key] = new
                result.add(new)
            }
        }
        return result
    }

    /** Words starting with an uppercase letter, appearing >= 2 times, length 3-30. */
    fun extractCandidateNames(text: String): List<String> {
        val freq = HashMap<String, Int>()
        for (match in namePattern.findAll(text)) {
            val token = match.value
            if (token in stopWords) continue
            freq[token] = (freq[token] ?: 0) + 1
        }
        return freq
            .filter { it.value >= 2 && it.key.length >= 3 }
            .keys
            .sorted()
    }

    private fun stableSlug(s: String): String =
        s.lowercase()
            .replace(Regex("""[^\p{L}\p{N}]+"""), "-")
            .trim('-')

    companion object {
        private val namePattern = Regex("""\b\p{Lu}\p{L}{1,29}\b""")

        private val stopWords: Set<String> = setOf(
            "The", "A", "An", "He", "She", "It", "They", "We", "I", "You",
            "Chapter", "Part", "Book", "Mr", "Mrs", "Ms", "Dr",
            "Глава", "Часть", "Книга", "Господин", "Госпожа"
        )
    }
}
