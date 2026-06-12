package com.bookmind.memory

import com.bookmind.core.model.BookID
import com.bookmind.core.model.Character
import com.bookmind.core.model.Chunk
import com.bookmind.core.model.Fact
import com.bookmind.core.model.Recap
import com.bookmind.persistence.dao.CharacterDao
import com.bookmind.persistence.dao.ChunkDao
import com.bookmind.persistence.dao.FactDao
import com.bookmind.persistence.dao.RecapDao
import com.bookmind.persistence.toDomain
import com.bookmind.persistence.toEntity
import javax.inject.Inject

/** = iOS `SQLiteMemoryWriter`. Room-backed [MemoryWriting]. */
class RoomMemoryWriter @Inject constructor(
    private val chunkDao: ChunkDao,
    private val recapDao: RecapDao,
    private val factDao: FactDao,
    private val characterDao: CharacterDao
) : MemoryWriting {

    override suspend fun writeChunks(chunks: List<Chunk>, chapterIndex: Int) =
        chunkDao.insertAll(chunks.map { it.toEntity(chapterIndex) })

    override suspend fun writeRecap(recap: Recap) = recapDao.insert(recap.toEntity())

    override suspend fun writeFacts(facts: List<Fact>) =
        factDao.insertAll(facts.map { it.toEntity() })

    override suspend fun upsertCharacters(characters: List<Character>) =
        characterDao.upsertAll(characters.map { it.toEntity() })

    override suspend fun loadCharacters(bookID: BookID): List<Character> =
        characterDao.loadForBook(bookID.raw).map { it.toDomain() }
}
