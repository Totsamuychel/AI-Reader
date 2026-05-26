import Foundation
import SharedModels

public protocol BookImporting: Sendable {
    func importBook(at url: URL) async throws -> Book
}

public protocol BookParsing: Sendable {
    func parseChapters(for book: Book) async throws -> [Chapter]
    func rawText(for chapter: Chapter, in book: Book) async throws -> String
}

public protocol ReaderSessionControlling: AnyObject {
    func open(book: Book) async throws
    func goToNextChapter() async
    func goToPreviousChapter() async
    func updateProgress(_ position: ReadingPosition) async
    var currentBook: Book? { get }
    var currentChapter: Chapter? { get }
    var chapters: [Chapter] { get }
}

public enum ReaderError: Error, Sendable, CustomStringConvertible {
    case unsupportedFormat(String)
    case fileNotReadable(URL)
    case parsingFailed(String)
    case bookNotOpen

    public var description: String {
        switch self {
        case .unsupportedFormat(let ext): return "Unsupported format: \(ext)"
        case .fileNotReadable(let url): return "File not readable: \(url.path)"
        case .parsingFailed(let reason): return "Parsing failed: \(reason)"
        case .bookNotOpen: return "No book is currently open"
        }
    }
}
