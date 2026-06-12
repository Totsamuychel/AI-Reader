package com.bookmind.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Indirection over MediaPipe's `LlmInference`, mirroring iOS `MediaPipeBridge`.
 * Lets the app run (with [EchoMediaPipeBridge]) before the ~1.5 GB model is
 * downloaded — see [com.bookmind.llm.ModelDownloadService].
 */
interface MediaPipeBridge {
    suspend fun load(config: LLMConfig)
    suspend fun generate(prompt: String, maxTokens: Int): String
    val isLoaded: Boolean
}

/** Deterministic stub for previews/tests and pre-model-download state. */
@Singleton
class EchoMediaPipeBridge @Inject constructor() : MediaPipeBridge {
    override val isLoaded: Boolean = true
    override suspend fun load(config: LLMConfig) {}
    override suspend fun generate(prompt: String, maxTokens: Int): String {
        val userBlock = prompt
            .substringAfter("<start_of_turn>user", "")
            .substringBefore("<end_of_turn>")
            .trim()
            .ifEmpty { "(empty)" }
        return "[stub-llm-reply] " + userBlock.take(minOf(maxTokens * 4, 1200))
    }
}

/** Real MediaPipe Gemma bridge. = iOS app-target MediaPipe integration. */
@Singleton
class MediaPipeGemmaBridge @Inject constructor(
    @ApplicationContext private val context: Context
) : MediaPipeBridge {

    private var inference: LlmInference? = null
    override val isLoaded: Boolean get() = inference != null

    override suspend fun load(config: LLMConfig) = withContext(Dispatchers.IO) {
        if (inference != null) return@withContext
        if (!File(config.modelPath).exists()) throw LLMError.ModelNotFound(config.modelPath)
        val options = LlmInferenceOptions.builder()
            .setModelPath(config.modelPath)
            .setMaxTokens(config.maxTokens)
            .setTopK(config.topK)
            .setTemperature(config.temperature.toFloat())
            .build()
        inference = LlmInference.createFromOptions(context, options)
    }

    override suspend fun generate(prompt: String, maxTokens: Int): String =
        withContext(Dispatchers.IO) {
            val engine = inference ?: throw LLMError.NotConfigured
            try {
                engine.generateResponse(prompt)
            } catch (t: Throwable) {
                throw LLMError.GenerationFailed(t.message ?: "unknown")
            }
        }
}
