import Foundation
import GRDB
import Persistence
import SharedModels

public final class SQLiteCharacterLookup: CharacterLookupService {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func findCharacters(
        matching query: String,
        bookID: BookID,
        currentChapterIndex: Int
    ) async throws -> [Character] {
        let q = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !q.isEmpty else { return [] }
        let like = "%\(q.lowercased())%"
        return try await db.pool.read { db in
            // Exact (case-insensitive) match first, then partial (lower(name) LIKE)
            let exact = try CharacterRecord
                .filter(Column("book_id") == bookID.rawValue)
                .filter(sql: "LOWER(canonical_name) = ?", arguments: [q.lowercased()])
                .filter(Column("first_chapter_index") <= currentChapterIndex)
                .fetchAll(db)
                .map { $0.toDomain() }
            if !exact.isEmpty { return exact }

            return try CharacterRecord
                .filter(Column("book_id") == bookID.rawValue)
                .filter(sql: """
                    (LOWER(canonical_name) LIKE ?
                     OR LOWER(aliases_json) LIKE ?)
                """, arguments: [like, like])
                .filter(Column("first_chapter_index") <= currentChapterIndex)
                .order(Column("first_chapter_index").desc)
                .limit(5)
                .fetchAll(db)
                .map { $0.toDomain() }
        }
    }
}

public final class SQLiteFactSearch: FactSearchService {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func searchFacts(
        query: String,
        bookID: BookID,
        maxChapterIndex: Int
    ) async throws -> [Fact] {
        let q = ftsQuery(from: query)
        guard !q.isEmpty else { return [] }
        return try await db.pool.read { db in
            let sql = """
                SELECT facts.* FROM facts
                JOIN facts_fts ON facts_fts.rowid = facts.rowid
                WHERE facts.book_id = ?
                  AND facts.chapter_index <= ?
                  AND facts_fts MATCH ?
                ORDER BY facts.chapter_index DESC
                LIMIT 10
            """
            return try FactRecord
                .fetchAll(db, sql: sql, arguments: [bookID.rawValue, maxChapterIndex, q])
                .map { $0.toDomain() }
        }
    }
}

public final class SQLiteChunkSearch: ChunkSearchService {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func searchChunks(
        query: String,
        bookID: BookID,
        maxChapterIndex: Int
    ) async throws -> [Chunk] {
        let q = ftsQuery(from: query)
        guard !q.isEmpty else { return [] }
        return try await db.pool.read { db in
            let sql = """
                SELECT chunks.* FROM chunks
                JOIN chunks_fts ON chunks_fts.rowid = chunks.rowid
                WHERE chunks.book_id = ?
                  AND chunks.chapter_index <= ?
                  AND chunks_fts MATCH ?
                ORDER BY chunks.chapter_index DESC
                LIMIT 5
            """
            return try ChunkRecord
                .fetchAll(db, sql: sql, arguments: [bookID.rawValue, maxChapterIndex, q])
                .map { $0.toDomain() }
        }
    }
}

public final class SQLiteRecapLookup: RecapLookupService {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func latestRecap(
        bookID: BookID,
        maxChapterIndex: Int
    ) async throws -> Recap? {
        try await db.pool.read { db in
            try RecapRecord
                .filter(Column("book_id") == bookID.rawValue)
                .filter(Column("chapter_index") <= maxChapterIndex)
                .order(Column("chapter_index").desc)
                .fetchOne(db)?
                .toDomain()
        }
    }
}

public final class SQLiteEventLookup: EventLookupService {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func recentEvents(
        bookID: BookID,
        maxChapterIndex: Int,
        limit: Int
    ) async throws -> [Event] {
        try await db.pool.read { db in
            try EventRecord
                .filter(Column("book_id") == bookID.rawValue)
                .filter(Column("chapter_index") <= maxChapterIndex)
                .order(Column("chapter_index").desc)
                .limit(limit)
                .fetchAll(db)
                .map { $0.toDomain() }
        }
    }
}

// MARK: - FTS Query Sanitizer

/// Sanitizes a free-text query for FTS5 MATCH usage. Splits into tokens,
/// removes operator chars, joins with `OR`.
func ftsQuery(from query: String) -> String {
    let allowed = CharacterSet.alphanumerics.union(.whitespaces)
        .union(CharacterSet(charactersIn: "-_"))
    let cleaned = String(query.unicodeScalars.filter { allowed.contains($0) })
    let tokens = cleaned
        .split(whereSeparator: { $0.isWhitespace })
        .map { String($0) }
        .filter { $0.count >= 2 }

    guard !tokens.isEmpty else { return "" }
    return tokens.map { "\"\($0)\"" }.joined(separator: " OR ")
}
