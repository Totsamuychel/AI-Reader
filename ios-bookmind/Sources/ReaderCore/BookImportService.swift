import Foundation
import SharedModels

public final class BookImportService: BookImporting {
    private let bookStore: BookStoring
    private let parserFactory: (BookFormat) -> BookParsing

    public init(
        bookStore: BookStoring,
        parserFactory: @escaping (BookFormat) -> BookParsing
    ) {
        self.bookStore = bookStore
        self.parserFactory = parserFactory
    }

    public func importBook(at url: URL) async throws -> Book {
        let ext = url.pathExtension.lowercased()
        guard let format = BookFormat(rawValue: ext) else {
            throw ReaderError.unsupportedFormat(ext)
        }
        let id = BookID(UUID().uuidString)
        let title = url.deletingPathExtension().lastPathComponent

        let book = Book(
            id: id,
            title: title,
            author: nil,
            format: format,
            fileURL: url
        )
        try await bookStore.save(book)

        let parser = parserFactory(format)
        let chapters = try await parser.parseChapters(for: book)
        try await bookStore.saveChapters(chapters, for: book.id)

        return book
    }
}
