import XCTest
@testable import LLM
@testable import Retrieval
@testable import Safety
import SharedModels

final class AnswerServiceTests: XCTestCase {
    func testAnswerGoesThroughBoundaryAndAssemblyAndLLM() async throws {
        let retrieval = StubRetrieval()
        let llm = CapturingLLM()
        let service = AnswerService(deps: .init(
            llm: llm,
            retrieval: retrieval,
            assembler: PromptContextAssembler(),
            boundary: SpoilerBoundaryResolver(),
            scanner: HeuristicSpoilerScanner(),
            bookTitle: { _ in "Test Book" },
            mode: { .safe }
        ))

        let result = try await service.answer(
            question: "Who is Alice?",
            bookID: BookID("b"),
            currentChapterIndex: 2
        )

        XCTAssertTrue(llm.capturedUserPrompt.contains("Who is Alice?"))
        XCTAssertTrue(llm.capturedSystemPrompt.contains("Test Book"))
        XCTAssertTrue(llm.capturedSystemPrompt.contains("Allowed knowledge horizon"))
        XCTAssertEqual(result, "All good.")
    }

    func testLeakTriggersRetryInSafeMode() async throws {
        let retrieval = StubRetrieval()
        let llm = SequenceLLM(replies: [
            "В главе 14 произойдет неожиданный поворот.", // spoiler
            "Без спойлеров: персонажи продолжают путь."
        ])
        let service = AnswerService(deps: .init(
            llm: llm,
            retrieval: retrieval,
            assembler: PromptContextAssembler(),
            boundary: SpoilerBoundaryResolver(),
            scanner: HeuristicSpoilerScanner(),
            bookTitle: { _ in "Test" },
            mode: { .safe }
        ))
        let result = try await service.answer(
            question: "Что произойдет дальше?",
            bookID: BookID("b"),
            currentChapterIndex: 1
        )
        XCTAssertEqual(llm.calls, 2)
        XCTAssertFalse(result.contains("главе 14"))
    }
}

private struct StubRetrieval: ContextRetrieving {
    func context(
        for question: String,
        bookID: BookID,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ) async throws -> RetrievalContext {
        RetrievalContext()
    }
}

private final class CapturingLLM: LLMClient, @unchecked Sendable {
    var capturedSystemPrompt = ""
    var capturedUserPrompt = ""
    func generate(systemPrompt: String, userPrompt: String, maxTokens: Int) async throws -> String {
        capturedSystemPrompt = systemPrompt
        capturedUserPrompt = userPrompt
        return "All good."
    }
}

private final class SequenceLLM: LLMClient, @unchecked Sendable {
    var replies: [String]
    var calls = 0
    init(replies: [String]) { self.replies = replies }
    func generate(systemPrompt: String, userPrompt: String, maxTokens: Int) async throws -> String {
        defer { calls += 1 }
        return replies[min(calls, replies.count - 1)]
    }
}
