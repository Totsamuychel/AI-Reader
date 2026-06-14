package com.bookmind.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bookmind.persistence.dao.BookDao
import com.bookmind.persistence.dao.CharacterDao
import com.bookmind.persistence.dao.ChunkDao
import com.bookmind.persistence.dao.EventDao
import com.bookmind.persistence.dao.FactDao
import com.bookmind.persistence.dao.ProgressDao
import com.bookmind.persistence.dao.ReadingSessionDao
import com.bookmind.persistence.dao.RecapDao
import com.bookmind.persistence.dao.ShelfDao
import com.bookmind.persistence.dao.UserQuoteDao
import com.bookmind.persistence.entity.BookEntity
import com.bookmind.persistence.entity.ChapterEntity
import com.bookmind.persistence.entity.CharacterEntity
import com.bookmind.persistence.entity.ChunkEntity
import com.bookmind.persistence.entity.ChunkFts
import com.bookmind.persistence.entity.EventEntity
import com.bookmind.persistence.entity.FactEntity
import com.bookmind.persistence.entity.FactFts
import com.bookmind.persistence.entity.ReadingProgressEntity
import com.bookmind.persistence.entity.ReadingSessionEntity
import com.bookmind.persistence.entity.RecapEntity
import com.bookmind.persistence.entity.RelationEntity
import com.bookmind.persistence.entity.ShelfEntity
import com.bookmind.persistence.entity.UserQuoteEntity

/** = iOS `DatabaseManager` + `Migrator`. Room manages the FTS sync triggers. */
@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ReadingProgressEntity::class,
        ChunkEntity::class,
        ChunkFts::class,
        CharacterEntity::class,
        EventEntity::class,
        RecapEntity::class,
        FactEntity::class,
        FactFts::class,
        RelationEntity::class,
        UserQuoteEntity::class,
        ShelfEntity::class,
        ReadingSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun progressDao(): ProgressDao
    abstract fun chunkDao(): ChunkDao
    abstract fun factDao(): FactDao
    abstract fun characterDao(): CharacterDao
    abstract fun recapDao(): RecapDao
    abstract fun eventDao(): EventDao
    abstract fun userQuoteDao(): UserQuoteDao
    abstract fun shelfDao(): ShelfDao
    abstract fun readingSessionDao(): ReadingSessionDao
}
