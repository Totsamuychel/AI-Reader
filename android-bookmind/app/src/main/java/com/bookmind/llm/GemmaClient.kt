package com.bookmind.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * = iOS `MediaPipeGemmaClient`. Wraps a [MediaPipeBridge], composing the
 * Gemma chat-turn prompt format around system/user content.
 */
@Singleton
class GemmaClient @Inject constructor(
    private val bridge: MediaPipeBridge,
    private val modelDownload: ModelDownloadService
) : LLMClient {

    private suspend fun ensureLoaded() {
        if (bridge.isLoaded) return
        bridge.load(
            LLMConfig(
                modelPath = modelDownload.modelFile.absolutePath,
                maxTokens = 1024,
                temperature = 0.8,
                topK = 40
            )
        )
    }

    private fun compose(systemPrompt: String, userPrompt: String): String =
        """
        <start_of_turn>system
        $systemPrompt
        <end_of_turn>
        <start_of_turn>user
        $userPrompt
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()

    override suspend fun generate(systemPrompt: String, userPrompt: String, maxTokens: Int): String {
        ensureLoaded()
        return bridge.generate(compose(systemPrompt, userPrompt), maxTokens)
    }

    /**
     * Streaming surface used by the UI. The current bridge generates
     * synchronously, so we emit the full response as a single token; swapping in
     * MediaPipe's async result listener upgrades this to token-by-token.
     */
    override fun generateStream(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int
    ): Flow<String> = flow {
        ensureLoaded()
        emit(bridge.generate(compose(systemPrompt, userPrompt), maxTokens))
    }
}
