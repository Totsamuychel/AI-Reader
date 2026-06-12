package com.bookmind.retrieval

import javax.inject.Inject

/** = iOS `PromptContextAssembler`. Renders the safe context block fed to the LLM. */
class PromptContextAssembler @Inject constructor() : PromptContextAssembling {

    override fun makePromptContext(
        retrievalContext: RetrievalContext,
        question: String,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ): String = buildString {
        append("## Reading state\n")
        append("- Current chapter index: $currentChapterIndex\n")
        append("- Allowed knowledge horizon (inclusive chapter index): $spoilerBoundary\n")
        append("- Anything not in the context below MUST be treated as unknown.\n\n")

        retrievalContext.safeRecap?.let { recap ->
            append("## Safe recap (chapter ${recap.chapterIndex})\n${recap.recapText}\n\n")
        }

        if (retrievalContext.characterCards.isNotEmpty()) {
            append("## Character cards\n")
            for (c in retrievalContext.characterCards.take(6)) {
                val sb = StringBuilder("- ${c.canonicalName}")
                if (c.aliases.isNotEmpty()) sb.append(" (aka ${c.aliases.joinToString(", ")})")
                sb.append(" — first seen ch.${c.firstChapterIndex}, known up to ch.${c.lastSafeChapterIndex}")
                c.safeSummary?.takeIf { it.isNotEmpty() }?.let { sb.append(": $it") }
                append(sb.toString()).append("\n")
            }
            append("\n")
        }

        if (retrievalContext.recentEvents.isNotEmpty()) {
            append("## Recent events (no spoilers past ch.$spoilerBoundary)\n")
            for (e in retrievalContext.recentEvents.take(5)) {
                append("- ch.${e.chapterIndex} — ${e.shortDescription}\n")
            }
            append("\n")
        }

        if (retrievalContext.facts.isNotEmpty()) {
            append("## Facts\n")
            for (f in retrievalContext.facts.take(8)) {
                append("- ch.${f.chapterIndex} [${f.factType}] ${f.text}\n")
            }
            append("\n")
        }

        if (retrievalContext.quotes.isNotEmpty()) {
            append("## Relevant quotes\n")
            for (q in retrievalContext.quotes.take(3)) {
                append("> ${q.text.take(280)}\n")
            }
            append("\n")
        }

        append("## User question\n$question\n")
    }
}
