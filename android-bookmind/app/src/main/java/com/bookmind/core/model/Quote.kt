package com.bookmind.core.model

/** Highlight colors offered when saving a quote. */
enum class HighlightColor(val rgb: Long) {
    YELLOW(0xFFFFF176),
    GREEN(0xFFA5D6A7),
    BLUE(0xFF90CAF9),
    PINK(0xFFF48FB1),
    PURPLE(0xFFCE93D8);

    companion object {
        fun fromName(name: String?): HighlightColor =
            entries.firstOrNull { it.name == name } ?: YELLOW
    }
}

/** A passage the reader highlighted and saved while reading. */
data class UserQuote(
    val id: String,
    val bookID: BookID,
    val chapterID: ChapterID?,
    val chapterIndex: Int,
    val text: String,
    val note: String? = null,
    val color: HighlightColor = HighlightColor.YELLOW,
    val createdAt: Long
)
