package com.bookmind.core.model

/** Typed identifiers mirroring the iOS `SharedModels` value types. */
@JvmInline
value class BookID(val raw: String)

@JvmInline
value class ChapterID(val raw: String)

@JvmInline
value class ChunkID(val raw: String)
