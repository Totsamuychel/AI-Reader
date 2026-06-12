package com.bookmind.memory

import com.bookmind.core.model.Chapter
import com.bookmind.core.model.Fact
import javax.inject.Inject

/** = iOS `HeuristicFactIndexer`. Character-appearance facts + light dialog snippets. */
class HeuristicFactIndexer @Inject constructor(
    private val characterDetector: HeuristicCharacterDetector
) : FactIndexing {

    override fun extractFacts(chapter: Chapter, rawText: String): List<Fact> {
        val facts = mutableListOf<Fact>()

        val names = characterDetector.extractCandidateNames(rawText)
        for (name in names) {
            facts.add(
                Fact(
                    id = "${chapter.id.raw}#appear-${name.lowercase()}",
                    bookID = chapter.bookID,
                    chapterID = chapter.id,
                    chapterIndex = chapter.index,
                    factType = "character_appearance",
                    subjectID = name,
                    objectID = null,
                    text = "Персонаж $name упомянут в главе ${chapter.index + 1}.",
                    spoilerLevel = 0
                )
            )
        }

        // First two quoted lines as light dialog snippets.
        val matches = quotePattern.findAll(rawText).take(2).toList()
        matches.forEachIndexed { i, m ->
            val quote = m.groupValues[1]
            facts.add(
                Fact(
                    id = "${chapter.id.raw}#quote-$i",
                    bookID = chapter.bookID,
                    chapterID = chapter.id,
                    chapterIndex = chapter.index,
                    factType = "dialog",
                    subjectID = null,
                    objectID = null,
                    text = quote,
                    spoilerLevel = 1
                )
            )
        }
        return facts
    }

    companion object {
        private val quotePattern = Regex("""[«"]([^«»"]{8,200})[»"]""")
    }
}
