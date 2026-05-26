import XCTest
@testable import ReaderCore
import SharedModels

final class TXTParserTests: XCTestCase {
    func testSplitChaptersDetectsChapterMarkers() async throws {
        let text = """
        Chapter 1 Beginnings
        Once upon a time there was a hobbit.

        Chapter 2 The Road
        He left the Shire on a misty morning.
        """
        let url = try makeTempFile(content: text)
        let book = Book(
            id: BookID("b1"),
            title: "Test",
            format: .txt,
            fileURL: url
        )
        let parser = TXTParser()
        let chapters = try await parser.parseChapters(for: book)
        XCTAssertEqual(chapters.count, 2)
        XCTAssertTrue(chapters[0].title?.contains("Chapter 1") ?? false)
        let body0 = try await parser.rawText(for: chapters[0], in: book)
        XCTAssertTrue(body0.contains("hobbit"))
    }

    func testSingleChapterFallback() async throws {
        let url = try makeTempFile(content: "Just a single piece of text with no markers.")
        let book = Book(id: BookID("b2"), title: "Plain", format: .txt, fileURL: url)
        let parser = TXTParser()
        let chapters = try await parser.parseChapters(for: book)
        XCTAssertEqual(chapters.count, 1)
        let body = try await parser.rawText(for: chapters[0], in: book)
        XCTAssertEqual(body, "Just a single piece of text with no markers.")
    }

    private func makeTempFile(content: String) throws -> URL {
        let url = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("txt")
        try content.write(to: url, atomically: true, encoding: .utf8)
        return url
    }
}
