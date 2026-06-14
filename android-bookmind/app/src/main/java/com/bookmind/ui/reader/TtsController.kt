package com.bookmind.ui.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Thin wrapper over Android [TextToSpeech] that reads a chapter sentence by
 * sentence and tracks which sentence is "current" so the reader can highlight it.
 */
class TtsController(context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var sentences: List<String> = emptyList()

    var isSpeaking by mutableStateOf(false)
        private set
    var currentSentence by mutableIntStateOf(-1)
        private set
    var speechRate by mutableStateOf(1.0f)
        private set

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            tts?.language = Locale.getDefault()
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.toIntOrNull()?.let { currentSentence = it }
            }

            override fun onDone(utteranceId: String?) {
                val idx = utteranceId?.toIntOrNull() ?: return
                if (idx >= sentences.lastIndex) {
                    isSpeaking = false
                    currentSentence = -1
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                isSpeaking = false
            }
        })
    }

    /** Splits [text] into sentences and starts speaking from [startIndex]. */
    fun start(text: String, startIndex: Int = 0) {
        if (!ready) return
        sentences = splitSentences(text)
        if (sentences.isEmpty()) return
        isSpeaking = true
        enqueueFrom(startIndex.coerceIn(0, sentences.lastIndex))
    }

    fun pause() {
        tts?.stop()
        isSpeaking = false
    }

    fun resume() {
        if (sentences.isEmpty()) return
        isSpeaking = true
        enqueueFrom(currentSentence.coerceAtLeast(0))
    }

    fun next() = jump(+1)
    fun previous() = jump(-1)

    fun setRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
        if (isSpeaking) enqueueFrom(currentSentence.coerceAtLeast(0))
    }

    private fun jump(delta: Int) {
        if (sentences.isEmpty()) return
        val target = (currentSentence + delta).coerceIn(0, sentences.lastIndex)
        isSpeaking = true
        enqueueFrom(target)
    }

    private fun enqueueFrom(index: Int) {
        val engine = tts ?: return
        engine.stop()
        engine.setSpeechRate(speechRate)
        for (i in index until sentences.size) {
            val mode = if (i == index) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            engine.speak(sentences[i], mode, null, i.toString())
        }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun splitSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?。！？])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}

/** Remembers a [TtsController] tied to the composition and shuts it down on dispose. */
@Composable
fun rememberTtsController(): TtsController {
    val context = LocalContext.current
    val controller = remember { TtsController(context) }
    DisposableEffect(Unit) {
        onDispose { controller.shutdown() }
    }
    return controller
}
