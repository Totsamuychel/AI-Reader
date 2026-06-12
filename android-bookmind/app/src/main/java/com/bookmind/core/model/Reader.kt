package com.bookmind.core.model

/** = iOS `ReadingPosition`. */
data class ReadingPosition(
    val bookID: BookID,
    val chapterID: ChapterID? = null,
    val progressFraction: Double = 0.0,
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReaderTheme { LIGHT, DARK, SEPIA }

/** = iOS `ReaderSettings`. */
data class ReaderSettings(
    val fontSize: Double = 17.0,
    val lineSpacing: Double = 1.4,
    val theme: ReaderTheme = ReaderTheme.LIGHT
) {
    companion object {
        val DEFAULT = ReaderSettings()
    }
}
