import Foundation

public protocol BookStoring: Sendable {
    func listBooks() async throws -> [Book]
    func save(_ book: Book) async throws
    func saveChapters(_ chapters: [Chapter], for bookID: BookID) async throws
    func chapters(for bookID: BookID) async throws -> [Chapter]
}

public protocol ReadingProgressStoring: Sendable {
    func loadPosition(for bookID: BookID) async throws -> ReadingPosition?
    func savePosition(_ position: ReadingPosition) async throws
}
