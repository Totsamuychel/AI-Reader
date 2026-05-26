import XCTest
@testable import ReaderCore
import SharedModels

final class ReadingProgressTests: XCTestCase {
    func testSaveAndLoadPosition() async throws {
        let store = InMemoryReadingProgressStore()
        let pos = ReadingPosition(
            bookID: BookID("b"),
            chapterID: ChapterID("c1"),
            progressFraction: 0.4
        )
        try await store.savePosition(pos)
        let loaded = try await store.loadPosition(for: BookID("b"))
        XCTAssertNotNil(loaded)
        XCTAssertEqual(loaded?.chapterID, ChapterID("c1"))
        XCTAssertEqual(loaded?.progressFraction ?? 0, 0.4, accuracy: 0.0001)
    }

    func testLoadMissingReturnsNil() async throws {
        let store = InMemoryReadingProgressStore()
        let loaded = try await store.loadPosition(for: BookID("unknown"))
        XCTAssertNil(loaded)
    }
}
