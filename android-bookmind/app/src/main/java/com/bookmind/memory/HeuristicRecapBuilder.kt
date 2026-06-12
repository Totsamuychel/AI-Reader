package com.bookmind.memory

import com.bookmind.core.model.Chapter
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.Recap
import javax.inject.Inject

/** = iOS `HeuristicRecapBuilder`. Leading sentences of the chapter, trimmed to a budget. */
class HeuristicRecapBuilder @Inject constructor() : SafeRecapBuilding {

    private val maxSentences = 4
    private val maxCharacters = 600

    override fun buildRecap(chapter: Chapter, chunks: List<Chunk>): Recap {
        val joined = chunks.take(2).joinToString(" ") { it.text }
        val firstSentences = Sentences.leading(joined, maxSentences)
        var text = firstSentences.joinToString(" ")
        if (text.length > maxCharacters) {
            text = text.substring(0, maxCharacters) + "…"
        }
        return Recap(
            id = "${chapter.id.raw}#recap",
            bookID = chapter.bookID,
            chapterID = chapter.id,
            chapterIndex = chapter.index,
            recapText = text,
            style = "safe"
        )
    }
}
