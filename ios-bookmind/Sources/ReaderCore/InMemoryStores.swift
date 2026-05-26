import Foundation
import SharedModels

public actor InMemoryReadingProgressStore: ReadingProgressStoring {
    private var positions: [BookID: ReadingPosition] = [:]
    public init() {}

    public func loadPosition(for bookID: BookID) async throws -> ReadingPosition? {
        positions[bookID]
    }

    public func savePosition(_ position: ReadingPosition) async throws {
        positions[position.bookID] = position
    }
}

public actor InMemoryBookStore: BookStoring {
    private var books: [BookID: Book] = [:]
    private var chaptersByBook: [BookID: [Chapter]] = [:]
    public init() {}

    public func listBooks() async throws -> [Book] {
        books.values.sorted { $0.addedAt > $1.addedAt }
    }

    public func save(_ book: Book) async throws {
        books[book.id] = book
    }

    public func saveChapters(_ chapters: [Chapter], for bookID: BookID) async throws {
        chaptersByBook[bookID] = chapters.sorted { $0.index < $1.index }
    }

    public func chapters(for bookID: BookID) async throws -> [Chapter] {
        chaptersByBook[bookID] ?? []
    }
}
