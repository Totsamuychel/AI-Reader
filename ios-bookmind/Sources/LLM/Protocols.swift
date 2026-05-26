import Foundation
import SharedModels

public protocol LLMClient: Sendable {
    func generate(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int
    ) async throws -> String
}

public protocol ConversationSession {
    func send(userMessage: String, context: String) async throws -> String
    func reset()
}

public protocol AnswerProviding: Sendable {
    func answer(
        question: String,
        bookID: BookID,
        currentChapterIndex: Int
    ) async throws -> String
}

public struct LLMConfig: Sendable {
    public var modelPath: URL
    public var maxTokens: Int
    public var temperature: Double
    public var topK: Int

    public init(
        modelPath: URL,
        maxTokens: Int = 512,
        temperature: Double = 0.6,
        topK: Int = 40
    ) {
        self.modelPath = modelPath
        self.maxTokens = maxTokens
        self.temperature = temperature
        self.topK = topK
    }
}

public enum LLMError: Error, Sendable, CustomStringConvertible {
    case modelNotFound(URL)
    case generationFailed(String)
    case notConfigured

    public var description: String {
        switch self {
        case .modelNotFound(let url): return "LLM model file not found at \(url.path)"
        case .generationFailed(let msg): return "Generation failed: \(msg)"
        case .notConfigured: return "LLM is not configured"
        }
    }
}
