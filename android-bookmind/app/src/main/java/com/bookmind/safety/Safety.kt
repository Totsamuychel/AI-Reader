package com.bookmind.safety

import javax.inject.Inject

/** = iOS `AnswerMode`. */
enum class AnswerMode(val raw: String) {
    SAFE("safe"),   // strict no-spoilers
    HINT("hint"),   // soft hints allowed
    FULL("full")    // spoilers permitted (e.g. user re-reading)
}

/** = iOS `SpoilerBoundaryResolving`. */
interface SpoilerBoundaryResolving {
    fun allowedChapterIndex(mode: AnswerMode, currentChapterIndex: Int): Int
}

/** = iOS `ResponseSpoilerScanning`. */
interface ResponseSpoilerScanning {
    fun containsObviousSpoiler(
        answer: String,
        currentChapterIndex: Int,
        knownEntities: List<String>
    ): Boolean
}

/** = iOS `SpoilerBoundaryResolver`. */
class SpoilerBoundaryResolver @Inject constructor() : SpoilerBoundaryResolving {
    override fun allowedChapterIndex(mode: AnswerMode, currentChapterIndex: Int): Int =
        when (mode) {
            AnswerMode.SAFE -> currentChapterIndex
            AnswerMode.HINT -> currentChapterIndex // hints still bound by current chapter
            AnswerMode.FULL -> Int.MAX_VALUE
        }
}

/** = iOS `HeuristicSpoilerScanner`. */
class HeuristicSpoilerScanner @Inject constructor() : ResponseSpoilerScanning {

    override fun containsObviousSpoiler(
        answer: String,
        currentChapterIndex: Int,
        knownEntities: List<String>
    ): Boolean {
        val lowered = answer.lowercase()
        if (spoilerPhrases.any { lowered.contains(it) }) return true

        // Future-chapter references like "in chapter 14" / "в главе 14" with N > current + 1.
        for (m in chapterRefPattern.findAll(answer)) {
            val n = m.groupValues[1].toIntOrNull() ?: 0
            if (n > currentChapterIndex + 1) return true
        }
        return false
    }

    companion object {
        private val spoilerPhrases = listOf(
            "in the end", "finally reveals", "turns out to be",
            "dies in", "kills", "betrays", "is the murderer",
            "marries", "the killer is",
            "в конце концов", "оказывается", "погибает", "предает",
            "убивает", "выходит замуж", "оказался"
        )

        private val chapterRefPattern =
            Regex("""(?:chapter|глав[аеы])\s*(\d{1,3})""", RegexOption.IGNORE_CASE)
    }
}
