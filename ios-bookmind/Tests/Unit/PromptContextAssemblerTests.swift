import XCTest
@testable import Retrieval
import SharedModels

final class PromptContextAssemblerTests: XCTestCase {
    func testAssemblesAllSections() {
        let bookID = BookID("b")
        let chapterID = ChapterID("ch1")
        let recap = Recap(
            id: "r1", bookID: bookID, chapterID: chapterID,
            chapterIndex: 0, recapText: "Intro recap"
        )
        let character = Character(
            id: "c", bookID: bookID,
            canonicalName: "Alice",
            firstChapterIndex: 0, lastSafeChapterIndex: 0,
            safeSummary: "Curious girl"
        )
        let fact = Fact(
            id: "f", bookID: bookID, chapterID: chapterID,
            chapterIndex: 0, factType: "appear",
            text: "Alice appears"
        )
        let context = RetrievalContext(
            safeRecap: recap,
            characterCards: [character],
            recentEvents: [],
            quotes: [],
            facts: [fact]
        )
        let prompt = PromptContextAssembler().makePromptContext(
            from: context,
            question: "Who is Alice?",
            currentChapterIndex: 0,
            spoilerBoundary: 0
        )
        XCTAssertTrue(prompt.contains("Alice"))
        XCTAssertTrue(prompt.contains("Intro recap"))
        XCTAssertTrue(prompt.contains("Who is Alice?"))
        XCTAssertTrue(prompt.contains("Allowed knowledge horizon"))
    }
}
