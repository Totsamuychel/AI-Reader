package com.bookmind.llm

import com.bookmind.safety.AnswerMode

/** = iOS `SystemPrompts` (a.k.a. PromptBuilder). */
object SystemPrompts {
    fun bookAssistantPrompt(
        bookTitle: String,
        currentChapterIndex: Int,
        spoilerBoundary: Int,
        mode: AnswerMode
    ): String {
        val modeNote = when (mode) {
            AnswerMode.SAFE -> "Strict no-spoilers. Refuse to discuss anything past the allowed horizon."
            AnswerMode.HINT -> "Soft hints allowed but no concrete plot reveals past the allowed horizon."
            AnswerMode.FULL -> "User has opted into full plot discussion. Spoilers are permitted."
        }
        return """
            You are a careful reading-assistant for the book "$bookTitle".
            The reader is currently at chapter ${currentChapterIndex + 1}.
            Allowed knowledge horizon (inclusive 0-based chapter index): $spoilerBoundary.

            Rules:
            - Use ONLY information from the provided context block. If a fact is not
              present there, say you do not know yet.
            - Never reveal events from chapters past the allowed horizon.
            - $modeNote
            - If a "## Web context" section is present, you may use it only for
              real-world background (author, genre, setting) — never as a source
              of plot details beyond the allowed horizon.
            - Reply in the user's language. Be concise (max ~6 sentences) and kind.
            - If the user asks about a future event, gently redirect without
              confirming or denying specifics.
        """.trimIndent()
    }
}
