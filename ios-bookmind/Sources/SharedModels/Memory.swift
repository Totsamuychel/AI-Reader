import Foundation

public struct Chunk: Identifiable, Codable, Sendable, Hashable {
    public let id: ChunkID
    public let bookID: BookID
    public let chapterID: ChapterID
    public let index: Int
    public let text: String
    public let tokenCount: Int
    public let spoilerLevel: Int

    public init(
        id: ChunkID,
        bookID: BookID,
        chapterID: ChapterID,
        index: Int,
        text: String,
        tokenCount: Int,
        spoilerLevel: Int = 0
    ) {
        self.id = id
        self.bookID = bookID
        self.chapterID = chapterID
        self.index = index
        self.text = text
        self.tokenCount = tokenCount
        self.spoilerLevel = spoilerLevel
    }
}

public struct Character: Identifiable, Codable, Sendable, Hashable {
    public let id: String
    public let bookID: BookID
    public let canonicalName: String
    public let aliases: [String]
    public let firstChapterIndex: Int
    public let lastSafeChapterIndex: Int
    public let safeSummary: String?

    public init(
        id: String,
        bookID: BookID,
        canonicalName: String,
        aliases: [String] = [],
        firstChapterIndex: Int,
        lastSafeChapterIndex: Int,
        safeSummary: String? = nil
    ) {
        self.id = id
        self.bookID = bookID
        self.canonicalName = canonicalName
        self.aliases = aliases
        self.firstChapterIndex = firstChapterIndex
        self.lastSafeChapterIndex = lastSafeChapterIndex
        self.safeSummary = safeSummary
    }
}

public struct Event: Identifiable, Codable, Sendable, Hashable {
    public let id: String
    public let bookID: BookID
    public let chapterID: ChapterID
    public let chapterIndex: Int
    public let eventType: String
    public let shortDescription: String
    public let longDescriptionSafe: String?
    public let spoilerLevel: Int

    public init(
        id: String,
        bookID: BookID,
        chapterID: ChapterID,
        chapterIndex: Int,
        eventType: String,
        shortDescription: String,
        longDescriptionSafe: String? = nil,
        spoilerLevel: Int = 0
    ) {
        self.id = id
        self.bookID = bookID
        self.chapterID = chapterID
        self.chapterIndex = chapterIndex
        self.eventType = eventType
        self.shortDescription = shortDescription
        self.longDescriptionSafe = longDescriptionSafe
        self.spoilerLevel = spoilerLevel
    }
}

public struct Relation: Identifiable, Codable, Sendable, Hashable {
    public let id: String
    public let bookID: BookID
    public let sourceCharacterID: String
    public let targetCharacterID: String
    public let relationType: String
    public let confidence: Double
    public let safeUntilChapterIndex: Int

    public init(
        id: String,
        bookID: BookID,
        sourceCharacterID: String,
        targetCharacterID: String,
        relationType: String,
        confidence: Double,
        safeUntilChapterIndex: Int
    ) {
        self.id = id
        self.bookID = bookID
        self.sourceCharacterID = sourceCharacterID
        self.targetCharacterID = targetCharacterID
        self.relationType = relationType
        self.confidence = confidence
        self.safeUntilChapterIndex = safeUntilChapterIndex
    }
}

public struct Recap: Identifiable, Codable, Sendable, Hashable {
    public let id: String
    public let bookID: BookID
    public let chapterID: ChapterID
    public let chapterIndex: Int
    public let recapText: String
    public let style: String

    public init(
        id: String,
        bookID: BookID,
        chapterID: ChapterID,
        chapterIndex: Int,
        recapText: String,
        style: String = "safe"
    ) {
        self.id = id
        self.bookID = bookID
        self.chapterID = chapterID
        self.chapterIndex = chapterIndex
        self.recapText = recapText
        self.style = style
    }
}

public struct Fact: Identifiable, Codable, Sendable, Hashable {
    public let id: String
    public let bookID: BookID
    public let chapterID: ChapterID
    public let chapterIndex: Int
    public let factType: String
    public let subjectID: String?
    public let objectID: String?
    public let text: String
    public let spoilerLevel: Int

    public init(
        id: String,
        bookID: BookID,
        chapterID: ChapterID,
        chapterIndex: Int,
        factType: String,
        subjectID: String? = nil,
        objectID: String? = nil,
        text: String,
        spoilerLevel: Int = 0
    ) {
        self.id = id
        self.bookID = bookID
        self.chapterID = chapterID
        self.chapterIndex = chapterIndex
        self.factType = factType
        self.subjectID = subjectID
        self.objectID = objectID
        self.text = text
        self.spoilerLevel = spoilerLevel
    }
}
