import Foundation
import GRDB
import SharedModels

// MARK: - Books

public struct BookRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "books"
    public var id: String
    public var title: String
    public var author: String?
    public var format: String
    public var file_url: String
    public var added_at: Date

    public init(book: Book) {
        self.id = book.id.rawValue
        self.title = book.title
        self.author = book.author
        self.format = book.format.rawValue
        self.file_url = book.fileURL.absoluteString
        self.added_at = book.addedAt
    }

    public func toDomain() -> Book {
        Book(
            id: BookID(id),
            title: title,
            author: author,
            format: BookFormat(rawValue: format) ?? .txt,
            fileURL: URL(string: file_url) ?? URL(fileURLWithPath: file_url),
            addedAt: added_at
        )
    }
}

// MARK: - Chapters

public struct ChapterRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "chapters"
    public var id: String
    public var book_id: String
    public var idx: Int
    public var title: String?
    public var content_ref: String?

    public init(chapter: Chapter) {
        self.id = chapter.id.rawValue
        self.book_id = chapter.bookID.rawValue
        self.idx = chapter.index
        self.title = chapter.title
        self.content_ref = chapter.contentRef?.absoluteString
    }

    public func toDomain() -> Chapter {
        Chapter(
            id: ChapterID(id),
            bookID: BookID(book_id),
            index: idx,
            title: title,
            contentRef: content_ref.flatMap { URL(string: $0) ?? URL(fileURLWithPath: $0) }
        )
    }
}

// MARK: - Reading Progress

public struct ReadingProgressRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "reading_progress"
    public var book_id: String
    public var chapter_id: String?
    public var progress_fraction: Double
    public var updated_at: Date

    public init(position: ReadingPosition) {
        self.book_id = position.bookID.rawValue
        self.chapter_id = position.chapterID?.rawValue
        self.progress_fraction = position.progressFraction
        self.updated_at = position.updatedAt
    }

    public func toDomain() -> ReadingPosition {
        ReadingPosition(
            bookID: BookID(book_id),
            chapterID: chapter_id.map { ChapterID($0) },
            progressFraction: progress_fraction,
            updatedAt: updated_at
        )
    }
}

// MARK: - Memory

public struct ChunkRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "chunks"
    public var id: String
    public var book_id: String
    public var chapter_id: String
    public var idx: Int
    public var text: String
    public var token_count: Int
    public var spoiler_level: Int
    public var chapter_index: Int

    public init(chunk: Chunk, chapterIndex: Int) {
        self.id = chunk.id.rawValue
        self.book_id = chunk.bookID.rawValue
        self.chapter_id = chunk.chapterID.rawValue
        self.idx = chunk.index
        self.text = chunk.text
        self.token_count = chunk.tokenCount
        self.spoiler_level = chunk.spoilerLevel
        self.chapter_index = chapterIndex
    }

    public func toDomain() -> Chunk {
        Chunk(
            id: ChunkID(id),
            bookID: BookID(book_id),
            chapterID: ChapterID(chapter_id),
            index: idx,
            text: text,
            tokenCount: token_count,
            spoilerLevel: spoiler_level
        )
    }
}

public struct CharacterRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "characters"
    public var id: String
    public var book_id: String
    public var canonical_name: String
    public var aliases_json: String
    public var first_chapter_index: Int
    public var last_safe_chapter_index: Int
    public var safe_summary: String?

    public init(character: Character) {
        self.id = character.id
        self.book_id = character.bookID.rawValue
        self.canonical_name = character.canonicalName
        self.aliases_json = (try? String(
            data: JSONEncoder().encode(character.aliases),
            encoding: .utf8
        )) ?? "[]"
        self.first_chapter_index = character.firstChapterIndex
        self.last_safe_chapter_index = character.lastSafeChapterIndex
        self.safe_summary = character.safeSummary
    }

    public func toDomain() -> Character {
        let aliases: [String] = (try? JSONDecoder().decode(
            [String].self,
            from: Data(aliases_json.utf8)
        )) ?? []
        return Character(
            id: id,
            bookID: BookID(book_id),
            canonicalName: canonical_name,
            aliases: aliases,
            firstChapterIndex: first_chapter_index,
            lastSafeChapterIndex: last_safe_chapter_index,
            safeSummary: safe_summary
        )
    }
}

public struct EventRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "events"
    public var id: String
    public var book_id: String
    public var chapter_id: String
    public var chapter_index: Int
    public var event_type: String
    public var short_description: String
    public var long_description_safe: String?
    public var spoiler_level: Int

    public init(event: Event) {
        self.id = event.id
        self.book_id = event.bookID.rawValue
        self.chapter_id = event.chapterID.rawValue
        self.chapter_index = event.chapterIndex
        self.event_type = event.eventType
        self.short_description = event.shortDescription
        self.long_description_safe = event.longDescriptionSafe
        self.spoiler_level = event.spoilerLevel
    }

    public func toDomain() -> Event {
        Event(
            id: id,
            bookID: BookID(book_id),
            chapterID: ChapterID(chapter_id),
            chapterIndex: chapter_index,
            eventType: event_type,
            shortDescription: short_description,
            longDescriptionSafe: long_description_safe,
            spoilerLevel: spoiler_level
        )
    }
}

public struct RecapRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "recaps"
    public var id: String
    public var book_id: String
    public var chapter_id: String
    public var chapter_index: Int
    public var recap_text: String
    public var style: String

    public init(recap: Recap) {
        self.id = recap.id
        self.book_id = recap.bookID.rawValue
        self.chapter_id = recap.chapterID.rawValue
        self.chapter_index = recap.chapterIndex
        self.recap_text = recap.recapText
        self.style = recap.style
    }

    public func toDomain() -> Recap {
        Recap(
            id: id,
            bookID: BookID(book_id),
            chapterID: ChapterID(chapter_id),
            chapterIndex: chapter_index,
            recapText: recap_text,
            style: style
        )
    }
}

public struct FactRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "facts"
    public var id: String
    public var book_id: String
    public var chapter_id: String
    public var chapter_index: Int
    public var fact_type: String
    public var subject_id: String?
    public var object_id: String?
    public var text: String
    public var spoiler_level: Int

    public init(fact: Fact) {
        self.id = fact.id
        self.book_id = fact.bookID.rawValue
        self.chapter_id = fact.chapterID.rawValue
        self.chapter_index = fact.chapterIndex
        self.fact_type = fact.factType
        self.subject_id = fact.subjectID
        self.object_id = fact.objectID
        self.text = fact.text
        self.spoiler_level = fact.spoilerLevel
    }

    public func toDomain() -> Fact {
        Fact(
            id: id,
            bookID: BookID(book_id),
            chapterID: ChapterID(chapter_id),
            chapterIndex: chapter_index,
            factType: fact_type,
            subjectID: subject_id,
            objectID: object_id,
            text: text,
            spoilerLevel: spoiler_level
        )
    }
}

public struct RelationRecord: Codable, FetchableRecord, PersistableRecord {
    public static let databaseTableName = "relations"
    public var id: String
    public var book_id: String
    public var source_character_id: String
    public var target_character_id: String
    public var relation_type: String
    public var confidence: Double
    public var safe_until_chapter_index: Int

    public init(relation: Relation) {
        self.id = relation.id
        self.book_id = relation.bookID.rawValue
        self.source_character_id = relation.sourceCharacterID
        self.target_character_id = relation.targetCharacterID
        self.relation_type = relation.relationType
        self.confidence = relation.confidence
        self.safe_until_chapter_index = relation.safeUntilChapterIndex
    }

    public func toDomain() -> Relation {
        Relation(
            id: id,
            bookID: BookID(book_id),
            sourceCharacterID: source_character_id,
            targetCharacterID: target_character_id,
            relationType: relation_type,
            confidence: confidence,
            safeUntilChapterIndex: safe_until_chapter_index
        )
    }
}
