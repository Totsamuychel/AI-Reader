import XCTest
@testable import Safety

final class SpoilerBoundaryTests: XCTestCase {
    func testSafeBoundaryIsCurrentChapter() {
        let resolver = SpoilerBoundaryResolver()
        XCTAssertEqual(resolver.allowedChapterIndex(for: .safe, currentChapterIndex: 5), 5)
        XCTAssertEqual(resolver.allowedChapterIndex(for: .hint, currentChapterIndex: 5), 5)
        XCTAssertEqual(resolver.allowedChapterIndex(for: .full, currentChapterIndex: 5), .max)
    }

    func testScannerDetectsForwardChapterReference() {
        let scanner = HeuristicSpoilerScanner()
        XCTAssertTrue(scanner.containsObviousSpoiler(
            "В главе 14 произойдет неожиданный поворот",
            currentChapterIndex: 3,
            knownEntities: []
        ))
        XCTAssertFalse(scanner.containsObviousSpoiler(
            "Герой грустит и идет домой.",
            currentChapterIndex: 3,
            knownEntities: []
        ))
    }

    func testScannerDetectsPlotTwistPhrase() {
        let scanner = HeuristicSpoilerScanner()
        XCTAssertTrue(scanner.containsObviousSpoiler(
            "Оказывается, он давно знал правду.",
            currentChapterIndex: 1,
            knownEntities: []
        ))
    }
}
