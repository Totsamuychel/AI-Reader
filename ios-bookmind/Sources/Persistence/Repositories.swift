import Foundation
import GRDB
import SharedModels

public protocol BookRepository: Sendable {
    func fetchAll() async throws -> [Book]
    func fetch(by id: BookID) async throws -> Book?
    func save(_ book: Book) async throws
    func delete(_ id: BookID) async throws
}

public protocol ChapterRepository: Sendable {
    func fetchChapters(for bookID: BookID) async throws -> [Chapter]
    func saveChapters(_ chapters: [Chapter]) async throws
}

public protocol ReadingProgressRepository: Sendable {
    func loadPosition(for bookID: BookID) async throws -> ReadingPosition?
    func savePosition(_ position: ReadingPosition) async throws
}

public final class SQLiteBookRepository: BookRepository {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func fetchAll() async throws -> [Book] {
        try await db.pool.read { db in
            try BookRecord
                .order(Column("added_at").desc)
                .fetchAll(db)
                .map { $0.toDomain() }
        }
    }

    public func fetch(by id: BookID) async throws -> Book? {
        try await db.pool.read { db in
            try BookRecord.fetchOne(db, key: id.rawValue)?.toDomain()
        }
    }

    public func save(_ book: Book) async throws {
        try await db.pool.write { db in
            try BookRecord(book: book).save(db)
        }
    }

    public func delete(_ id: BookID) async throws {
        _ = try await db.pool.write { db in
            try BookRecord.deleteOne(db, key: id.rawValue)
        }
    }
}

public final class SQLiteChapterRepository: ChapterRepository {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func fetchChapters(for bookID: BookID) async throws -> [Chapter] {
        try await db.pool.read { db in
            try ChapterRecord
                .filter(Column("book_id") == bookID.rawValue)
                .order(Column("idx"))
                .fetchAll(db)
                .map { $0.toDomain() }
        }
    }

    public func saveChapters(_ chapters: [Chapter]) async throws {
        try await db.pool.write { db in
            for c in chapters {
                try ChapterRecord(chapter: c).save(db)
            }
        }
    }
}

public final class SQLiteReadingProgressRepository: ReadingProgressRepository {
    private let db: DatabaseManager
    public init(db: DatabaseManager) { self.db = db }

    public func loadPosition(for bookID: BookID) async throws -> ReadingPosition? {
        try await db.pool.read { db in
            try ReadingProgressRecord.fetchOne(db, key: bookID.rawValue)?.toDomain()
        }
    }

    public func savePosition(_ position: ReadingPosition) async throws {
        try await db.pool.write { db in
            try ReadingProgressRecord(position: position).save(db)
        }
    }
}
