package com.bookmind.core.model

enum class BookFormat(val raw: String) {
    EPUB("epub"),
    FB2("fb2"),
    PDF("pdf"),
    TXT("txt");

    companion object {
        fun fromRaw(raw: String): BookFormat =
            entries.firstOrNull { it.raw == raw } ?: TXT

        /** Maps a file extension (without dot) to a format, defaulting to TXT. */
        fun fromExtension(ext: String): BookFormat = when (ext.lowercase()) {
            "epub" -> EPUB
            "fb2" -> FB2
            "pdf" -> PDF
            "txt" -> TXT
            else -> TXT
        }
    }
}

/** = iOS `Book`. */
data class Book(
    val id: BookID,
    val title: String,
    val author: String? = null,
    val format: BookFormat,
    val fileUri: String,
    val addedAt: Long = System.currentTimeMillis(),
    /** Null = not filed under any named shelf. */
    val shelfId: String? = null
)

/** A user-created named shelf books can be grouped under. */
data class Shelf(
    val id: String,
    val name: String,
    val colorRgb: Long,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/** = iOS `Chapter`. */
data class Chapter(
    val id: ChapterID,
    val bookID: BookID,
    val index: Int,
    val title: String? = null,
    val contentRef: String? = null
)
