package com.bookmind.llm

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * = android.md "Критическое отличие от iOS: загрузка модели".
 * The ~1.5 GB Gemma weights cannot ship in the APK, so they are downloaded
 * into private storage on first launch.
 */
@Singleton
class ModelDownloadService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val modelFile: File get() = File(context.filesDir, MODEL_FILE_NAME)
    val isModelPresent: Boolean get() = modelFile.exists() && modelFile.length() > 0

    sealed interface DownloadProgress {
        data class Downloading(val bytesRead: Long, val totalBytes: Long) : DownloadProgress {
            val fraction: Float get() = if (totalBytes > 0) bytesRead.toFloat() / totalBytes else 0f
        }
        data class Completed(val path: String) : DownloadProgress
        data class Failed(val message: String) : DownloadProgress
    }

    fun downloadModel(sourceUrl: String = DEFAULT_MODEL_URL): Flow<DownloadProgress> = flow {
        if (isModelPresent) {
            emit(DownloadProgress.Completed(modelFile.absolutePath))
            return@flow
        }
        val tmp = File(context.filesDir, "$MODEL_FILE_NAME.part")
        try {
            val connection = (URL(sourceUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 30_000
            }
            connection.connect()
            if (connection.responseCode !in 200..299) {
                emit(DownloadProgress.Failed("HTTP ${connection.responseCode}"))
                return@flow
            }
            val total = connection.contentLengthLong
            connection.inputStream.use { input ->
                tmp.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var readTotal = 0L
                    var read = input.read(buffer)
                    while (read != -1) {
                        output.write(buffer, 0, read)
                        readTotal += read
                        emit(DownloadProgress.Downloading(readTotal, total))
                        read = input.read(buffer)
                    }
                }
            }
            if (!tmp.renameTo(modelFile)) {
                emit(DownloadProgress.Failed("Could not finalize model file"))
                return@flow
            }
            emit(DownloadProgress.Completed(modelFile.absolutePath))
        } catch (t: Throwable) {
            tmp.delete()
            emit(DownloadProgress.Failed(t.message ?: "Download failed"))
        }
    }.flowOn(Dispatchers.IO)

    companion object {
        const val MODEL_FILE_NAME = "gemma-2b-it-cpu-int4.bin"
        // Replace with your Firebase Storage / CDN URL.
        const val DEFAULT_MODEL_URL = "https://example.com/models/$MODEL_FILE_NAME"
    }
}
