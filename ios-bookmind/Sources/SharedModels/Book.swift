import Foundation

public enum BookFormat: String, Codable, Sendable, CaseIterable {
    case epub
    case txt
}

public struct Book: Identifiable, Codable, Sendable, Hashable {
    public let id: BookID
    public var title: String
    public var author: String?
    public var format: BookFormat
    public var fileURL: URL
    public var addedAt: Date

    public init(
        id: BookID,
        title: String,
        author: String? = nil,
        format: BookFormat,
        fileURL: URL,
        addedAt: Date = Date()
    ) {
        self.id = id
        self.title = title
        self.author = author
        self.format = format
        self.fileURL = fileURL
        self.addedAt = addedAt
    }
}

public struct Chapter: Identifiable, Codable, Sendable, Hashable {
    public let id: ChapterID
    public let bookID: BookID
    public let index: Int
    public var title: String?
    public var contentRef: URL?

    public init(
        id: ChapterID,
        bookID: BookID,
        index: Int,
        title: String? = nil,
        contentRef: URL? = nil
    ) {
        self.id = id
        self.bookID = bookID
        self.index = index
        self.title = title
        self.contentRef = contentRef
    }
}
