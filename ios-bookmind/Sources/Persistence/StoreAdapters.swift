import Foundation
import SharedModels

/// Concrete `BookStoring` implementation backed by `BookRepository` + `ChapterRepository`.
public final class RepositoryBookStore: BookStoring {
    private let books: BookRepository
    private let chaptersRepo: ChapterRepository

    public init(books: BookRepository, chapters: ChapterRepository) {
        self.books = books
        self.chaptersRepo = chapters
    }

    public func listBooks() async throws -> [Book] {
        try await books.fetchAll()
    }

    public func save(_ book: Book) async throws {
        try await books.save(book)
    }

    public func saveChapters(_ chapters: [Chapter], for bookID: BookID) async throws {
        try await chaptersRepo.saveChapters(chapters)
    }

    public func chapters(for bookID: BookID) async throws -> [Chapter] {
        try await chaptersRepo.fetchChapters(for: bookID)
    }
}

/// Concrete `ReadingProgressStoring` implementation backed by SQLite.
public final class RepositoryReadingProgressStore: ReadingProgressStoring {
    private let progress: ReadingProgressRepository
    public init(progress: ReadingProgressRepository) { self.progress = progress }

    public func loadPosition(for bookID: BookID) async throws -> ReadingPosition? {
        try await progress.loadPosition(for: bookID)
    }

    public func savePosition(_ position: ReadingPosition) async throws {
        try await progress.savePosition(position)
    }
}
