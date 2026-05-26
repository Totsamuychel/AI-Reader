import XCTest
@testable import BookMemory
import SharedModels

final class ChunkerAndRecapTests: XCTestCase {
    private func makeChapter() -> Chapter {
        Chapter(id: ChapterID("c#0"), bookID: BookID("b"), index: 0, title: "First")
    }

    func testChunkerSplitsLongTextIntoMultipleChunks() {
        let chunker = FixedSizeChunker(targetTokenCount: 30, overlapTokenCount: 4)
        let text = String(repeating: "This is a sentence. ", count: 30)
        let chunks = chunker.makeChunks(for: makeChapter(), rawText: text)
        XCTAssertGreaterThan(chunks.count, 1)
        XCTAssertTrue(chunks.allSatisfy { !$0.text.isEmpty })
    }

    func testChunkerProducesAtLeastOneChunkForShortText() {
        let chunker = FixedSizeChunker()
        let chunks = chunker.makeChunks(for: makeChapter(), rawText: "Hello world. Short text.")
        XCTAssertEqual(chunks.count, 1)
        XCTAssertTrue(chunks[0].text.contains("Hello"))
    }

    func testRecapKeepsLeadingSentences() {
        let chunker = FixedSizeChunker(targetTokenCount: 100, overlapTokenCount: 0)
        let recap = HeuristicRecapBuilder(maxSentences: 2, maxCharacters: 200)
        let chunks = chunker.makeChunks(
            for: makeChapter(),
            rawText: "First sentence. Second sentence here. Third one is longer."
        )
        let r = recap.buildRecap(for: makeChapter(), chunks: chunks)
        XCTAssertTrue(r.recapText.contains("First sentence"))
    }
}
