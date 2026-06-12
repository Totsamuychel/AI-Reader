package com.bookmind.llm

import com.bookmind.core.model.BookID
import kotlinx.coroutines.flow.Flow

/** = iOS `LLMClient`. */
interface LLMClient {
    suspend fun generate(systemPrompt: String, userPrompt: String, maxTokens: Int): String
    fun generateStream(systemPrompt: String, userPrompt: String, maxTokens: Int): Flow<String>
}

/** = iOS `AnswerProviding`. */
interface AnswerProviding {
    suspend fun answer(question: String, bookID: BookID, currentChapterIndex: Int): String
    fun answerStream(question: String, bookID: BookID, currentChapterIndex: Int): Flow<AnswerEvent>
}

/** Streaming events surfaced to the UI. = android.md `AnswerEvent`. */
sealed interface AnswerEvent {
    data class ContextRetrieved(val itemCount: Int) : AnswerEvent
    data class Token(val token: String) : AnswerEvent
    data class Complete(val fullText: String, val spoilerFlagged: Boolean) : AnswerEvent
    data class Failed(val message: String) : AnswerEvent
}

/** = iOS `LLMConfig`. */
data class LLMConfig(
    val modelPath: String,
    val maxTokens: Int = 512,
    val temperature: Double = 0.6,
    val topK: Int = 40
)

/** = iOS `LLMError`. */
sealed class LLMError(message: String) : Exception(message) {
    class ModelNotFound(path: String) : LLMError("LLM model file not found at $path")
    class GenerationFailed(msg: String) : LLMError("Generation failed: $msg")
    object NotConfigured : LLMError("LLM is not configured")
}
