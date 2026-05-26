import Foundation

/// MediaPipe LLM Inference (Gemma 4 E2B) adapter.
///
/// The MediaPipe iOS SDK is integrated at the app target level via SwiftPM
/// or CocoaPods (`MediaPipeTasksGenAI`). This wrapper is intentionally
/// SDK-agnostic so the SPM Core can build on macOS hosts and in tests; the
/// real SDK calls live behind a runtime-injected `MediaPipeBridge`.
public final class MediaPipeGemmaClient: LLMClient {
    public let config: LLMConfig
    private let bridge: MediaPipeBridge

    public init(config: LLMConfig, bridge: MediaPipeBridge) throws {
        guard FileManager.default.fileExists(atPath: config.modelPath.path) else {
            throw LLMError.modelNotFound(config.modelPath)
        }
        self.config = config
        self.bridge = bridge
        try bridge.load(config: config)
    }

    public func generate(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int
    ) async throws -> String {
        let composed = """
        <start_of_turn>system
        \(systemPrompt)
        <end_of_turn>
        <start_of_turn>user
        \(userPrompt)
        <end_of_turn>
        <start_of_turn>model
        """
        return try await bridge.generate(prompt: composed, maxTokens: maxTokens)
    }
}

/// Indirection over MediaPipe's `LlmInference` so the package compiles
/// without the SDK on non-iOS hosts.
public protocol MediaPipeBridge: Sendable {
    func load(config: LLMConfig) throws
    func generate(prompt: String, maxTokens: Int) async throws -> String
}

/// Stub bridge — useful for previews/tests; returns deterministic echoes.
public struct EchoMediaPipeBridge: MediaPipeBridge {
    public init() {}
    public func load(config: LLMConfig) throws {}
    public func generate(prompt: String, maxTokens: Int) async throws -> String {
        // Strip control turns; echo only the user content.
        let userBlock = prompt
            .components(separatedBy: "<start_of_turn>user")
            .dropFirst()
            .first?
            .components(separatedBy: "<end_of_turn>")
            .first?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? "(empty)"
        let trimmed = String(userBlock.prefix(min(maxTokens * 4, 1200)))
        return "[stub-llm-reply] " + trimmed
    }
}
