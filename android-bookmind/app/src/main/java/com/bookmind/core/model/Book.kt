package com.bookmind.core.model

enum class BookFormat(val raw: String) {
    EPUB("epub"),
    TXT("txt");

    companion object {
        fun fromRaw(raw: String): BookFormat =
            entries.firstOrNull { it.raw == raw } ?: TXT
    }
}

/** = iOS `Book`. */
data class Book(
    val id: BookID,
    val title: String,
    val author: String? = null,
    val format: BookFormat,
    val fileUri: String,
    val addedAt: Long = System.currentTimeMillis()
)

/** = iOS `Chapter`. */
data class Chapter(
    val id: ChapterID,
    val bookID: BookID,
    val index: Int,
    val title: String? = null,
    val contentRef: String? = null
)
