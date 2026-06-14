package com.bookmind.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookmind.settings.AiModel
import com.bookmind.settings.AnswerLanguage
import com.bookmind.settings.AppSettings
import com.bookmind.settings.PageAnimation
import com.bookmind.settings.ReaderBackground
import com.bookmind.settings.ScrollDirection
import com.bookmind.llm.ModelDownloadService
import com.bookmind.settings.SecureKeyStore
import com.bookmind.settings.SettingsStore
import com.bookmind.sync.SyncRepository
import com.bookmind.ui.theme.AppTheme
import com.bookmind.ui.theme.ReaderFont
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared across the app: exposes the persisted [AppSettings] and the setters
 * for the settings screen / theme selector. The reader/library also read it.
 */
/** On-device Gemma model availability + download progress for the settings UI. */
data class ModelStatus(
    val present: Boolean = false,
    val downloading: Boolean = false,
    val fraction: Float = 0f,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val secureKeyStore: SecureKeyStore,
    private val syncRepository: SyncRepository,
    private val modelDownload: ModelDownloadService
) : ViewModel() {

    val settings: StateFlow<AppSettings> = store.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppSettings()
    )

    private val _modelStatus = MutableStateFlow(ModelStatus(present = modelDownload.isModelPresent))
    val modelStatus: StateFlow<ModelStatus> = _modelStatus.asStateFlow()

    /** Downloads the on-device model, streaming progress into [modelStatus]. */
    fun downloadModel() {
        if (_modelStatus.value.downloading) return
        _modelStatus.update { it.copy(downloading = true, message = "Загрузка…") }
        viewModelScope.launch {
            modelDownload.downloadModel().collect { progress ->
                when (progress) {
                    is ModelDownloadService.DownloadProgress.Downloading -> _modelStatus.update {
                        it.copy(downloading = true, fraction = progress.fraction, message = null)
                    }
                    is ModelDownloadService.DownloadProgress.Completed -> _modelStatus.update {
                        it.copy(present = true, downloading = false, fraction = 1f, message = "Модель готова")
                    }
                    is ModelDownloadService.DownloadProgress.Failed -> _modelStatus.update {
                        it.copy(downloading = false, message = "Ошибка: ${progress.message}")
                    }
                }
            }
        }
    }

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    /** API key for the currently selected model, kept in sync with the secure store. */
    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    init {
        viewModelScope.launch {
            settings.collect { _apiKey.value = secureKeyStore.apiKey(it.aiModel) }
        }
    }

    fun setAiModel(model: AiModel) = launch {
        store.setAiModel(model)
        _apiKey.value = secureKeyStore.apiKey(model)
    }

    fun setAnswerLanguage(language: AnswerLanguage) = launch { store.setAnswerLanguage(language) }
    fun setOllamaUrl(url: String) = launch { store.setOllamaUrl(url) }

    fun setApiKey(key: String) {
        val model = settings.value.aiModel
        secureKeyStore.setApiKey(model, key)
        _apiKey.value = key
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching { syncRepository.export(uri) }
            .onSuccess { _syncMessage.value = "Backed up ${it.progressCount} progress + ${it.quoteCount} quotes" }
            .onFailure { _syncMessage.value = "Backup failed: ${it.message}" }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching { syncRepository.import(uri) }
            .onSuccess { _syncMessage.value = "Restored ${it.progressCount} progress + ${it.quoteCount} quotes" }
            .onFailure { _syncMessage.value = "Restore failed: ${it.message}" }
    }

    fun consumeSyncMessage() { _syncMessage.value = null }

    fun setTheme(theme: AppTheme) = launch { store.setTheme(theme) }
    fun setDynamicColor(enabled: Boolean) = launch { store.setDynamicColor(enabled) }
    fun setEInk(enabled: Boolean) = launch { store.setEInk(enabled) }
    fun setFont(font: ReaderFont) = launch { store.setFont(font) }
    fun setFontSize(sp: Float) = launch { store.setFontSize(sp) }
    fun setLineSpacing(value: Float) = launch { store.setLineSpacing(value) }
    fun setPageAnimation(anim: PageAnimation) = launch { store.setPageAnimation(anim) }
    fun setScrollDirection(dir: ScrollDirection) = launch { store.setScrollDirection(dir) }
    fun setReaderBackground(bg: ReaderBackground) = launch { store.setReaderBackground(bg) }
    fun setWarmth(value: Float) = launch { store.setWarmth(value) }
    fun setAutoAnalyze(enabled: Boolean) = launch { store.setAutoAnalyze(enabled) }
    fun setKeepHistory(enabled: Boolean) = launch { store.setKeepHistory(enabled) }
    fun setReaderWeight(weight: Float) = launch { store.setReaderWeight(weight) }
    fun setOnboardingComplete(complete: Boolean) = launch { store.setOnboardingComplete(complete) }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
