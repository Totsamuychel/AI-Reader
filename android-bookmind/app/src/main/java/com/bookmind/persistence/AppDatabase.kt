package com.bookmind.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bookmind.persistence.dao.BookDao
import com.bookmind.persistence.dao.ChatMessageDao
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
import com.bookmind.persistence.entity.ChatMessageEntity
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
        ReadingSessionEntity::class,
        ChatMessageEntity::class
    ],
    version = 6,
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
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        /** v5: user-supplied book cover image URI. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE books ADD COLUMN coverUri TEXT")
            }
        }

        /** v6: per-book AI chat history. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS chat_messages (
                        id TEXT NOT NULL PRIMARY KEY,
                        bookId TEXT NOT NULL,
                        role TEXT NOT NULL,
                        content TEXT NOT NULL,
                        spoilerFlagged INTEGER NOT NULL DEFAULT 0,
                        timestamp INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_chat_messages_bookId_timestamp " +
                        "ON chat_messages (bookId, timestamp)"
                )
            }
        }
    }
}
