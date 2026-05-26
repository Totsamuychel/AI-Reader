import Foundation
import GRDB
import Persistence
import SharedModels

/// SQLite-backed `MemoryWriting` implementation.
public final class SQLiteMemoryWriter: MemoryWriting {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func writeChunks(_ chunks: [Chunk], chapterIndex: Int) async throws {
        try await db.pool.write { db in
            for c in chunks {
                try ChunkRecord(chunk: c, chapterIndex: chapterIndex).save(db)
            }
        }
    }

    public func writeRecap(_ recap: Recap) async throws {
        try await db.pool.write { db in
            try RecapRecord(recap: recap).save(db)
        }
    }

    public func writeFacts(_ facts: [Fact]) async throws {
        try await db.pool.write { db in
            for f in facts {
                try FactRecord(fact: f).save(db)
            }
        }
    }

    public func upsertCharacters(_ characters: [Character]) async throws {
        try await db.pool.write { db in
            for c in characters {
                try CharacterRecord(character: c).save(db)
            }
        }
    }

    public func loadCharacters(for bookID: BookID) async throws -> [Character] {
        try await db.pool.read { db in
            try CharacterRecord
                .filter(Column("book_id") == bookID.rawValue)
                .fetchAll(db)
                .map { $0.toDomain() }
        }
    }
}
