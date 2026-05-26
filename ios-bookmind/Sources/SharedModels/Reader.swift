import Foundation

public struct ReadingPosition: Codable, Sendable, Hashable {
    public let bookID: BookID
    public var chapterID: ChapterID?
    public var progressFraction: Double
    public var updatedAt: Date

    public init(
        bookID: BookID,
        chapterID: ChapterID? = nil,
        progressFraction: Double = 0,
        updatedAt: Date = Date()
    ) {
        self.bookID = bookID
        self.chapterID = chapterID
        self.progressFraction = progressFraction
        self.updatedAt = updatedAt
    }
}

public enum ReaderTheme: String, Codable, Sendable, CaseIterable {
    case light
    case dark
    case sepia
}

public struct ReaderSettings: Codable, Sendable, Hashable {
    public var fontSize: Double
    public var lineSpacing: Double
    public var theme: ReaderTheme

    public init(
        fontSize: Double = 17,
        lineSpacing: Double = 1.4,
        theme: ReaderTheme = .light
    ) {
        self.fontSize = fontSize
        self.lineSpacing = lineSpacing
        self.theme = theme
    }

    public static let `default` = ReaderSettings()
}
