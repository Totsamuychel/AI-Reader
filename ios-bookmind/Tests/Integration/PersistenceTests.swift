import XCTest
@testable import Persistence
import SharedModels

final class PersistenceTests: XCTestCase {
    func testRepositoryRoundTrip() async throws {
        let db = try DatabaseManager.inMemory()
        let bookRepo = SQLiteBookRepository(db: db)
        let chapterRepo = SQLiteChapterRepository(db: db)
        let progressRepo = SQLiteReadingProgressRepository(db: db)

        let book = Book(
            id: BookID("b1"),
            title: "Roundtrip",
            author: "Author",
            format: .txt,
            fileURL: URL(fileURLWithPath: "/tmp/file.txt")
        )
        try await bookRepo.save(book)
        let all = try await bookRepo.fetchAll()
        XCTAssertEqual(all.count, 1)
        XCTAssertEqual(all.first?.title, "Roundtrip")

        let chapters = (0..<3).map {
            Chapter(id: ChapterID("c#\($0)"), bookID: book.id, index: $0, title: "Ch\($0)")
        }
        try await chapterRepo.saveChapters(chapters)
        let loaded = try await chapterRepo.fetchChapters(for: book.id)
        XCTAssertEqual(loaded.count, 3)
        XCTAssertEqual(loaded.map(\.index), [0, 1, 2])

        let pos = ReadingPosition(
            bookID: book.id,
            chapterID: ChapterID("c#1"),
            progressFraction: 0.33
        )
        try await progressRepo.savePosition(pos)
        let back = try await progressRepo.loadPosition(for: book.id)
        XCTAssertEqual(back?.chapterID, ChapterID("c#1"))
        XCTAssertEqual(back?.progressFraction ?? 0, 0.33, accuracy: 0.0001)
    }
}
