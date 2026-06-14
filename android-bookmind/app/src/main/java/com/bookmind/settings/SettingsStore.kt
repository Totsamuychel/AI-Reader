package com.bookmind.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bookmind.ui.theme.AppTheme
import com.bookmind.ui.theme.ReaderFont
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Page-turn animation styles. = plan's PageTurnAnimation sealed class. */
enum class PageAnimation(val displayName: String) {
    SLIDE("Slide"), FADE("Fade"), CURL("Curl"), NONE("None");

    companion object {
        fun fromName(name: String?): PageAnimation =
            entries.firstOrNull { it.name == name } ?: SLIDE
    }
}

enum class ScrollDirection { HORIZONTAL, VERTICAL;
    companion object {
        // Default to paged (swipe) reading.
        fun fromName(name: String?): ScrollDirection =
            entries.firstOrNull { it.name == name } ?: HORIZONTAL
    }
}

/**
 * Reading surface background. SYSTEM follows the app theme; the rest pin a fixed
 * page color (with a matching text color) regardless of the global theme, the way
 * dedicated readers offer Sepia / paper-white / dark presets.
 */
enum class ReaderBackground(
    val displayName: String,
    val bgArgb: Long?,
    val fgArgb: Long?
) {
    SYSTEM("Системный", null, null),
    WHITE("Белый", 0xFFFFFFFF, 0xFF1A1A1A),
    SEPIA("Бежевый", 0xFFF5ECD9, 0xFF40341E),
    GRAY("Тёмно-серый", 0xFF2B2B2B, 0xFFE6E1D8),
    BLACK("Чёрный", 0xFF000000, 0xFFC9C4BC);

    companion object {
        fun fromName(name: String?): ReaderBackground =
            entries.firstOrNull { it.name == name } ?: SYSTEM
    }
}

/** AI provider the assistant should target. Local Gemma is the only one wired to
 *  inference today; the rest persist credentials/config for future routing. */
enum class AiModel(val displayName: String, val needsApiKey: Boolean, val needsUrl: Boolean) {
    GEMMA_LOCAL("Gemma (on-device)", false, false),
    GEMINI("Gemini", true, false),
    CLAUDE("Claude", true, false),
    GPT4O("GPT-4o", true, false),
    OLLAMA("Ollama (local server)", false, true);

    companion object {
        fun fromName(name: String?): AiModel =
            entries.firstOrNull { it.name == name } ?: GEMMA_LOCAL
    }
}

enum class AnswerLanguage(val displayName: String) {
    AUTO("Auto"), RUSSIAN("Русский"), UKRAINIAN("Українська"), ENGLISH("English");

    companion object {
        fun fromName(name: String?): AnswerLanguage =
            entries.firstOrNull { it.name == name } ?: AUTO
    }
}

/** All persisted user preferences in one immutable snapshot. */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
    val eInkMode: Boolean = false,
    val readerFont: ReaderFont = ReaderFont.SERIF,
    val fontSizeSp: Float = 17f,
    val lineSpacing: Float = 1.5f,
    val pageAnimation: PageAnimation = PageAnimation.SLIDE,
    val scrollDirection: ScrollDirection = ScrollDirection.HORIZONTAL,
    val readerBackground: ReaderBackground = ReaderBackground.SYSTEM,
    /** Warm "night" tint over the page, 0f (off) .. 1f (strongest). */
    val warmth: Float = 0f,
    val autoAnalyzeSelection: Boolean = false,
    val keepChatHistory: Boolean = true,
    val aiModel: AiModel = AiModel.GEMMA_LOCAL,
    val answerLanguage: AnswerLanguage = AnswerLanguage.AUTO,
    val ollamaUrl: String = "",
    /** Reader/AI split: fraction of width given to the book in landscape. */
    val readerWeight: Float = 0.65f,
    val onboardingComplete: Boolean = false
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bookmind_settings")

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val eInk = booleanPreferencesKey("eink_mode")
        val font = stringPreferencesKey("reader_font")
        val fontSize = floatPreferencesKey("font_size_sp")
        val lineSpacing = floatPreferencesKey("line_spacing")
        val pageAnim = stringPreferencesKey("page_animation")
        val scrollDir = stringPreferencesKey("scroll_direction")
        val readerBackground = stringPreferencesKey("reader_background")
        val warmth = floatPreferencesKey("warmth_level")
        val autoAnalyze = booleanPreferencesKey("auto_analyze")
        val keepHistory = booleanPreferencesKey("keep_history")
        val aiModel = stringPreferencesKey("ai_model")
        val answerLanguage = stringPreferencesKey("answer_language")
        val ollamaUrl = stringPreferencesKey("ollama_url")
        val readerWeight = floatPreferencesKey("reader_weight")
        val onboarding = booleanPreferencesKey("onboarding_complete")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = AppTheme.fromName(p[Keys.theme]),
            dynamicColor = p[Keys.dynamicColor] ?: true,
            eInkMode = p[Keys.eInk] ?: false,
            readerFont = ReaderFont.fromName(p[Keys.font]),
            fontSizeSp = p[Keys.fontSize] ?: 17f,
            lineSpacing = p[Keys.lineSpacing] ?: 1.5f,
            pageAnimation = PageAnimation.fromName(p[Keys.pageAnim]),
            scrollDirection = ScrollDirection.fromName(p[Keys.scrollDir]),
            readerBackground = ReaderBackground.fromName(p[Keys.readerBackground]),
            warmth = p[Keys.warmth] ?: 0f,
            autoAnalyzeSelection = p[Keys.autoAnalyze] ?: false,
            keepChatHistory = p[Keys.keepHistory] ?: true,
            aiModel = AiModel.fromName(p[Keys.aiModel]),
            answerLanguage = AnswerLanguage.fromName(p[Keys.answerLanguage]),
            ollamaUrl = p[Keys.ollamaUrl] ?: "",
            readerWeight = p[Keys.readerWeight] ?: 0.65f,
            onboardingComplete = p[Keys.onboarding] ?: false
        )
    }

    suspend fun setTheme(theme: AppTheme) = edit { it[Keys.theme] = theme.name }
    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.dynamicColor] = enabled }
    suspend fun setEInk(enabled: Boolean) = edit { it[Keys.eInk] = enabled }
    suspend fun setFont(font: ReaderFont) = edit { it[Keys.font] = font.name }
    suspend fun setFontSize(sp: Float) = edit { it[Keys.fontSize] = sp }
    suspend fun setLineSpacing(value: Float) = edit { it[Keys.lineSpacing] = value }
    suspend fun setPageAnimation(anim: PageAnimation) = edit { it[Keys.pageAnim] = anim.name }
    suspend fun setScrollDirection(dir: ScrollDirection) = edit { it[Keys.scrollDir] = dir.name }
    suspend fun setReaderBackground(bg: ReaderBackground) = edit { it[Keys.readerBackground] = bg.name }
    suspend fun setWarmth(value: Float) = edit { it[Keys.warmth] = value }
    suspend fun setAutoAnalyze(enabled: Boolean) = edit { it[Keys.autoAnalyze] = enabled }
    suspend fun setKeepHistory(enabled: Boolean) = edit { it[Keys.keepHistory] = enabled }
    suspend fun setAiModel(model: AiModel) = edit { it[Keys.aiModel] = model.name }
    suspend fun setAnswerLanguage(language: AnswerLanguage) = edit { it[Keys.answerLanguage] = language.name }
    suspend fun setOllamaUrl(url: String) = edit { it[Keys.ollamaUrl] = url }
    suspend fun setReaderWeight(weight: Float) = edit { it[Keys.readerWeight] = weight }
    suspend fun setOnboardingComplete(complete: Boolean) = edit { it[Keys.onboarding] = complete }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}
