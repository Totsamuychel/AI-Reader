package com.bookmind.llm

import com.bookmind.core.model.BookID
import com.bookmind.persistence.BookStoring
import com.bookmind.retrieval.ContextRetrieving
import com.bookmind.retrieval.PromptContextAssembling
import com.bookmind.retrieval.WebSearching
import com.bookmind.safety.AnswerMode
import com.bookmind.safety.ResponseSpoilerScanning
import com.bookmind.safety.SpoilerBoundaryResolving
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * = iOS `AnswerService`. Orchestrates retrieval → prompt assembly → generation,
 * with a spoiler-scan + safe-mode regeneration pass.
 */
@Singleton
class AnswerService @Inject constructor(
    private val llm: LLMClient,
    private val retrieval: ContextRetrieving,
    private val assembler: PromptContextAssembling,
    private val boundary: SpoilerBoundaryResolving,
    private val scanner: ResponseSpoilerScanning,
    private val bookStore: BookStoring,
    private val webSearch: WebSearching
) : AnswerProviding {

    private val modeRef = AtomicReference(AnswerMode.SAFE)
    private val webEnabledRef = AtomicBoolean(false)

    /** UI-controlled spoiler mode. */
    var mode: AnswerMode
        get() = modeRef.get()
        set(value) = modeRef.set(value)

    /** UI-controlled internet tool. Off by default: web results can spoil. */
    var webEnabled: Boolean
        get() = webEnabledRef.get()
        set(value) = webEnabledRef.set(value)

    override suspend fun answer(
        question: String,
        bookID: BookID,
        currentChapterIndex: Int
    ): String {
        val mode = this.mode
        val boundaryIndex = boundary.allowedChapterIndex(mode, currentChapterIndex)

        val context = retrieval.context(question, bookID, currentChapterIndex, boundaryIndex)
        var userPrompt = assembler.makePromptContext(context, question, currentChapterIndex, boundaryIndex)

        val book = bookStore.book(bookID)
        val title = book?.title ?: "this book"

        if (webEnabled) {
            val snippet = runCatching {
                webSearch.search(listOfNotNull(title, book?.author, question).joinToString(" "))
            }.getOrNull()
            if (snippet != null) {
                userPrompt += "\n\n## Web context (external background; NEVER use it to reveal " +
                    "plot beyond the allowed horizon)\nSource: ${snippet.sourceUrl}\n${snippet.text}\n"
            }
        }

        val system = SystemPrompts.bookAssistantPrompt(title, currentChapterIndex, boundaryIndex, mode)

        val firstAnswer = llm.generate(system, userPrompt, maxTokens = 512)

        val knownEntities = context.characterCards.map { it.canonicalName }
        val leakDetected = scanner.containsObviousSpoiler(firstAnswer, currentChapterIndex, knownEntities)

        if (!leakDetected || mode == AnswerMode.FULL) return firstAnswer

        // Regenerate once in stricter safe mode if a leak was found.
        val safeBoundary = boundary.allowedChapterIndex(AnswerMode.SAFE, currentChapterIndex)
        val safeSystem = SystemPrompts.bookAssistantPrompt(title, currentChapterIndex, safeBoundary, AnswerMode.SAFE)
        return llm.generate(
            safeSystem,
            userPrompt + "\n\n[Reviewer note: previous answer leaked future events; rewrite without spoilers.]",
            maxTokens = 512
        )
    }

    override fun answerStream(
        question: String,
        bookID: BookID,
        currentChapterIndex: Int
    ): Flow<AnswerEvent> = flow {
        try {
            val boundaryIndex = boundary.allowedChapterIndex(mode, currentChapterIndex)
            val context = retrieval.context(question, bookID, currentChapterIndex, boundaryIndex)
            emit(
                AnswerEvent.ContextRetrieved(
                    context.characterCards.size + context.facts.size +
                        context.quotes.size + context.recentEvents.size
                )
            )

            // Spoiler-safe final text (includes the safe-mode regeneration pass).
            val finalText = answer(question, bookID, currentChapterIndex)
            val flagged = scanner.containsObviousSpoiler(
                finalText,
                currentChapterIndex,
                context.characterCards.map { it.canonicalName }
            )
            emit(AnswerEvent.Token(finalText))
            emit(AnswerEvent.Complete(finalText, spoilerFlagged = flagged && mode != AnswerMode.FULL))
        } catch (t: Throwable) {
            emit(AnswerEvent.Failed(t.message ?: "Generation failed"))
        }
    }
}
