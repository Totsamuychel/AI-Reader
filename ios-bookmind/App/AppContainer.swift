import Foundation
import SwiftUI

import BookMemory
import LLM
import Persistence
import ReaderCore
import Retrieval
import Safety
import SharedModels

/// Lightweight DI container. Held as an `@EnvironmentObject` across the app.
@MainActor
final class AppContainer: ObservableObject {
    // Settings
    @Published var readerSettings = ReaderSettings.default
    @Published var answerMode: AnswerMode = .safe

    // Core services
    let dbManager: DatabaseManager
    let bookStore: BookStoring
    let progressStore: ReadingProgressStoring
    let session: ReaderSessionController
    let bookImport: BookImportService
    let ingestion: BookIngesting
    let answer: AnswerProviding

    init(
        dbManager: DatabaseManager,
        bookStore: BookStoring,
        progressStore: ReadingProgressStoring,
        session: ReaderSessionController,
        bookImport: BookImportService,
        ingestion: BookIngesting,
        answer: AnswerProviding
    ) {
        self.dbManager = dbManager
        self.bookStore = bookStore
        self.progressStore = progressStore
        self.session = session
        self.bookImport = bookImport
        self.ingestion = ingestion
        self.answer = answer
    }

    static func makeDefault() -> AppContainer {
        let dbURL = FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask)
            .first!
            .appendingPathComponent("BookMind/library.sqlite")

        let dbManager: DatabaseManager
        do {
            dbManager = try DatabaseManager(url: dbURL)
        } catch {
            fatalError("Database init failed: \(error)")
        }

        let bookRepo = SQLiteBookRepository(db: dbManager)
        let chapterRepo = SQLiteChapterRepository(db: dbManager)
        let progressRepo = SQLiteReadingProgressRepository(db: dbManager)

        let bookStore = RepositoryBookStore(books: bookRepo, chapters: chapterRepo)
        let progressStore = RepositoryReadingProgressStore(progress: progressRepo)

        let parserFactory: (BookFormat) -> BookParsing = { format in
            switch format {
            case .epub: return EPUBParser()
            case .txt:  return TXTParser()
            }
        }

        let session = ReaderSessionController(
            bookStore: bookStore,
            progressStore: progressStore,
            parserFactory: parserFactory
        )

        let importService = BookImportService(
            bookStore: bookStore,
            parserFactory: parserFactory
        )

        // BookMemory pipeline
        let memoryWriter = SQLiteMemoryWriter(db: dbManager)
        let chunker = FixedSizeChunker()
        let recapBuilder = HeuristicRecapBuilder()
        let detector = HeuristicCharacterDetector()
        let factIndexer = HeuristicFactIndexer(characterDetector: detector)

        let textProvider = ParserBackedTextProvider(parserFactory: parserFactory)
        let ingestion = BookIngestionService(
            textProvider: textProvider,
            chunker: chunker,
            recapBuilder: recapBuilder,
            factIndexer: factIndexer,
            characterDetector: detector,
            memory: memoryWriter
        )

        // Retrieval
        let retrieval = RetrievalEngine(
            characters: SQLiteCharacterLookup(db: dbManager),
            facts: SQLiteFactSearch(db: dbManager),
            chunks: SQLiteChunkSearch(db: dbManager),
            recaps: SQLiteRecapLookup(db: dbManager),
            events: SQLiteEventLookup(db: dbManager)
        )

        // LLM (stub bridge by default — replace with real MediaPipe in iOS target).
        let modelURL = FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask)
            .first!
            .appendingPathComponent("BookMind/models/gemma-4-e2b.task")
        let llmConfig = LLMConfig(modelPath: modelURL)
        let llmClient: LLMClient = {
            if let real = try? MediaPipeGemmaClient(config: llmConfig, bridge: EchoMediaPipeBridge()) {
                return real
            }
            // Fallback: pure echo client wrapped as LLMClient.
            return FallbackEchoLLM()
        }()

        let assembler = PromptContextAssembler()
        let boundary = SpoilerBoundaryResolver()
        let scanner = HeuristicSpoilerScanner()

        let titleLookup: (BookID) async -> String? = { id in
            try? await bookRepo.fetch(by: id)?.title
        }

        // Captured-by-app state — read mode at call time.
        let modeAccessor: () -> AnswerMode = {
            // Default to safe; AppContainer publishes mode but the closure is
            // captured at construction time. Bridge via a singleton holder.
            return AnswerModeHolder.shared.current
        }

        let answer = AnswerService(deps: AnswerService.Dependencies(
            llm: llmClient,
            retrieval: retrieval,
            assembler: assembler,
            boundary: boundary,
            scanner: scanner,
            bookTitle: titleLookup,
            mode: modeAccessor
        ))

        return AppContainer(
            dbManager: dbManager,
            bookStore: bookStore,
            progressStore: progressStore,
            session: session,
            bookImport: importService,
            ingestion: ingestion,
            answer: answer
        )
    }
}

// MARK: - Helpers

/// Mode holder — kept outside the container so the closure capture stays safe.
final class AnswerModeHolder: @unchecked Sendable {
    static let shared = AnswerModeHolder()
    private let lock = NSLock()
    private var mode: AnswerMode = .safe
    var current: AnswerMode {
        lock.lock(); defer { lock.unlock() }
        return mode
    }
    func set(_ mode: AnswerMode) {
        lock.lock(); defer { lock.unlock() }
        self.mode = mode
    }
}

struct ParserBackedTextProvider: RawChapterTextProviding {
    let parserFactory: @Sendable (BookFormat) -> BookParsing
    func rawText(for chapter: Chapter, in book: Book) async throws -> String {
        try await parserFactory(book.format).rawText(for: chapter, in: book)
    }
}

struct FallbackEchoLLM: LLMClient {
    func generate(systemPrompt: String, userPrompt: String, maxTokens: Int) async throws -> String {
        "[fallback] " + String(userPrompt.suffix(300))
    }
}
