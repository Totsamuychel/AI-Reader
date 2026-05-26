import Foundation
import SharedModels

public protocol RawChapterTextProviding: Sendable {
    func rawText(for chapter: Chapter, in book: Book) async throws -> String
}

public final class BookIngestionService: BookIngesting {
    private let textProvider: RawChapterTextProviding
    private let chunker: ChapterChunking
    private let recapBuilder: SafeRecapBuilding
    private let factIndexer: FactIndexing
    private let characterDetector: CharacterDetecting
    private let memory: MemoryWriting

    public init(
        textProvider: RawChapterTextProviding,
        chunker: ChapterChunking,
        recapBuilder: SafeRecapBuilding,
        factIndexer: FactIndexing,
        characterDetector: CharacterDetecting,
        memory: MemoryWriting
    ) {
        self.textProvider = textProvider
        self.chunker = chunker
        self.recapBuilder = recapBuilder
        self.factIndexer = factIndexer
        self.characterDetector = characterDetector
        self.memory = memory
    }

    public func ingest(book: Book, chapters: [Chapter]) async throws {
        var knownCharacters = try await memory.loadCharacters(for: book.id)

        for chapter in chapters.sorted(by: { $0.index < $1.index }) {
            let rawText = try await textProvider.rawText(for: chapter, in: book)
            guard !rawText.isEmpty else { continue }

            let chunks = chunker.makeChunks(for: chapter, rawText: rawText)
            try await memory.writeChunks(chunks, chapterIndex: chapter.index)

            let recap = recapBuilder.buildRecap(for: chapter, chunks: chunks)
            try await memory.writeRecap(recap)

            let facts = factIndexer.extractFacts(from: chapter, rawText: rawText)
            try await memory.writeFacts(facts)

            let detected = characterDetector.detectCharacters(
                in: chapter,
                rawText: rawText,
                existing: knownCharacters
            )
            try await memory.upsertCharacters(detected)
            // Refresh in-memory cache for next chapter.
            knownCharacters = try await memory.loadCharacters(for: book.id)
        }
    }
}
