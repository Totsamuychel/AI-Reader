import Foundation
import Retrieval
import Safety
import SharedModels

public final class AnswerService: AnswerProviding {
    public struct Dependencies: Sendable {
        public var llm: LLMClient
        public var retrieval: ContextRetrieving
        public var assembler: PromptContextAssembling
        public var boundary: SpoilerBoundaryResolving
        public var scanner: ResponseSpoilerScanning
        public var bookTitle: (BookID) async -> String?
        public var mode: () -> AnswerMode

        public init(
            llm: LLMClient,
            retrieval: ContextRetrieving,
            assembler: PromptContextAssembling,
            boundary: SpoilerBoundaryResolving,
            scanner: ResponseSpoilerScanning,
            bookTitle: @escaping (BookID) async -> String?,
            mode: @escaping () -> AnswerMode
        ) {
            self.llm = llm
            self.retrieval = retrieval
            self.assembler = assembler
            self.boundary = boundary
            self.scanner = scanner
            self.bookTitle = bookTitle
            self.mode = mode
        }
    }

    private let deps: Dependencies

    public init(deps: Dependencies) { self.deps = deps }

    public func answer(
        question: String,
        bookID: BookID,
        currentChapterIndex: Int
    ) async throws -> String {
        let mode = deps.mode()
        let boundary = deps.boundary.allowedChapterIndex(
            for: mode,
            currentChapterIndex: currentChapterIndex
        )

        let context = try await deps.retrieval.context(
            for: question,
            bookID: bookID,
            currentChapterIndex: currentChapterIndex,
            spoilerBoundary: boundary
        )

        let userPrompt = deps.assembler.makePromptContext(
            from: context,
            question: question,
            currentChapterIndex: currentChapterIndex,
            spoilerBoundary: boundary
        )

        let title = await deps.bookTitle(bookID) ?? "this book"
        let system = SystemPrompts.bookAssistantPrompt(
            bookTitle: title,
            currentChapterIndex: currentChapterIndex,
            spoilerBoundary: boundary,
            mode: mode
        )

        let firstAnswer = try await deps.llm.generate(
            systemPrompt: system,
            userPrompt: userPrompt,
            maxTokens: 512
        )

        let knownEntities = context.characterCards.map(\.canonicalName)
        let leakDetected = deps.scanner.containsObviousSpoiler(
            firstAnswer,
            currentChapterIndex: currentChapterIndex,
            knownEntities: knownEntities
        )

        guard leakDetected, mode != .full else {
            return firstAnswer
        }

        // Regenerate once in stricter safe mode if a leak was found.
        let safeBoundary = deps.boundary.allowedChapterIndex(
            for: .safe,
            currentChapterIndex: currentChapterIndex
        )
        let safeSystem = SystemPrompts.bookAssistantPrompt(
            bookTitle: title,
            currentChapterIndex: currentChapterIndex,
            spoilerBoundary: safeBoundary,
            mode: .safe
        )
        let retry = try await deps.llm.generate(
            systemPrompt: safeSystem,
            userPrompt: userPrompt + "\n\n[Reviewer note: previous answer leaked future events; rewrite without spoilers.]",
            maxTokens: 512
        )
        return retry
    }
}
