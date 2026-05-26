import XCTest
@testable import BookMemory
@testable import Persistence
@testable import Retrieval
@testable import Safety
import SharedModels

final class IngestionAndRetrievalTests: XCTestCase {
    private var db: DatabaseManager!
    private var memory: SQLiteMemoryWriter!

    override func setUp() async throws {
        try await super.setUp()
        db = try DatabaseManager.inMemory()
        memory = SQLiteMemoryWriter(db: db)

        // Seed a book so foreign keys hold.
        let bookRepo = SQLiteBookRepository(db: db)
        try await bookRepo.save(Book(
            id: BookID("book-1"),
            title: "Test",
            format: .txt,
            fileURL: URL(fileURLWithPath: "/tmp/test.txt")
        ))
    }

    func testIngestionPopulatesMemoryAndRespectsBoundary() async throws {
        let book = Book(id: BookID("book-1"), title: "Test", format: .txt, fileURL: URL(fileURLWithPath: "/tmp/test.txt"))
        let chapters = [
            Chapter(id: ChapterID("c#0"), bookID: book.id, index: 0, title: "Beginnings"),
            Chapter(id: ChapterID("c#1"), bookID: book.id, index: 1, title: "Twist"),
            Chapter(id: ChapterID("c#2"), bookID: book.id, index: 2, title: "Ending")
        ]
        let texts: [ChapterID: String] = [
            ChapterID("c#0"): "Alice met Bob in the garden. Alice greeted Bob warmly.",
            ChapterID("c#1"): "Bob revealed a strange map to Alice. Alice studied the map carefully.",
            ChapterID("c#2"): "Bob betrays Alice in the final confrontation. Alice grieves."
        ]

        let provider = TestTextProvider(texts: texts)
        let service = BookIngestionService(
            textProvider: provider,
            chunker: FixedSizeChunker(targetTokenCount: 60, overlapTokenCount: 10),
            recapBuilder: HeuristicRecapBuilder(maxSentences: 2),
            factIndexer: HeuristicFactIndexer(),
            characterDetector: HeuristicCharacterDetector(),
            memory: memory
        )

        try await service.ingest(book: book, chapters: chapters)

        // After ingestion: characters and facts should exist.
        let characters = try await memory.loadCharacters(for: book.id)
        XCTAssertFalse(characters.isEmpty, "Expected at least one detected character")

        // Retrieval should not surface chapter-2 content when capped at chapter 0.
        let engine = RetrievalEngine(
            characters: SQLiteCharacterLookup(db: db),
            facts: SQLiteFactSearch(db: db),
            chunks: SQLiteChunkSearch(db: db),
            recaps: SQLiteRecapLookup(db: db),
            events: SQLiteEventLookup(db: db)
        )
        let ctx = try await engine.context(
            for: "Alice",
            bookID: book.id,
            currentChapterIndex: 0,
            spoilerBoundary: 0
        )
        XCTAssertTrue(ctx.facts.allSatisfy { $0.chapterIndex <= 0 })
        XCTAssertTrue(ctx.quotes.allSatisfy { ($0.spoilerLevel) <= 1 })
        XCTAssertNotNil(ctx.safeRecap)
        XCTAssertEqual(ctx.safeRecap?.chapterIndex, 0)
    }
}

private struct TestTextProvider: RawChapterTextProviding {
    let texts: [ChapterID: String]
    func rawText(for chapter: Chapter, in book: Book) async throws -> String {
        texts[chapter.id] ?? ""
    }
}
