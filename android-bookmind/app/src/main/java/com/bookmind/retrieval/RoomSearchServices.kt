package com.bookmind.retrieval

import com.bookmind.core.model.BookID
import com.bookmind.core.model.Character
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.Event
import com.bookmind.core.model.Fact
import com.bookmind.core.model.Recap
import com.bookmind.persistence.dao.CharacterDao
import com.bookmind.persistence.dao.ChunkDao
import com.bookmind.persistence.dao.EventDao
import com.bookmind.persistence.dao.FactDao
import com.bookmind.persistence.dao.RecapDao
import com.bookmind.persistence.toDomain
import javax.inject.Inject

/** = iOS `SQLiteCharacterLookup`. */
class RoomCharacterLookup @Inject constructor(
    private val dao: CharacterDao
) : CharacterLookupService {
    override suspend fun findCharacters(
        query: String,
        bookID: BookID,
        currentChapterIndex: Int
    ): List<Character> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val exact = dao.findExact(bookID.raw, q.lowercase(), currentChapterIndex).map { it.toDomain() }
        if (exact.isNotEmpty()) return exact
        val like = "%${q.lowercase()}%"
        return dao.findPartial(bookID.raw, like, currentChapterIndex).map { it.toDomain() }
    }
}

/** = iOS `SQLiteFactSearch`. */
class RoomFactSearch @Inject constructor(
    private val dao: FactDao
) : FactSearchService {
    override suspend fun searchFacts(query: String, bookID: BookID, maxChapterIndex: Int): List<Fact> {
        val q = ftsQuery(query)
        if (q.isEmpty()) return emptyList()
        return dao.searchSafe(q, bookID.raw, maxChapterIndex).map { it.toDomain() }
    }
}

/** = iOS `SQLiteChunkSearch`. */
class RoomChunkSearch @Inject constructor(
    private val dao: ChunkDao
) : ChunkSearchService {
    override suspend fun searchChunks(query: String, bookID: BookID, maxChapterIndex: Int): List<Chunk> {
        val q = ftsQuery(query)
        if (q.isEmpty()) return emptyList()
        return dao.searchSafe(q, bookID.raw, maxChapterIndex).map { it.toDomain() }
    }
}

/** = iOS `SQLiteRecapLookup`. */
class RoomRecapLookup @Inject constructor(
    private val dao: RecapDao
) : RecapLookupService {
    override suspend fun latestRecap(bookID: BookID, maxChapterIndex: Int): Recap? =
        dao.latestRecap(bookID.raw, maxChapterIndex)?.toDomain()
}

/** = iOS `SQLiteEventLookup`. */
class RoomEventLookup @Inject constructor(
    private val dao: EventDao
) : EventLookupService {
    override suspend fun recentEvents(bookID: BookID, maxChapterIndex: Int, limit: Int): List<Event> =
        dao.recentEvents(bookID.raw, maxChapterIndex, limit).map { it.toDomain() }
}
