package com.bookmind.core.model

/** A passage the reader highlighted and saved while reading. */
data class UserQuote(
    val id: String,
    val bookID: BookID,
    val chapterID: ChapterID?,
    val chapterIndex: Int,
    val text: String,
    val note: String? = null,
    val createdAt: Long
)
