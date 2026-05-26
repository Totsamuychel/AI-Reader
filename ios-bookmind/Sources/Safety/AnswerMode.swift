import Foundation

public enum AnswerMode: String, Codable, Sendable, CaseIterable {
    case safe   // strict no-spoilers
    case hint   // soft hints allowed
    case full   // spoilers permitted (e.g. user re-reading)
}

public protocol SpoilerBoundaryResolving: Sendable {
    func allowedChapterIndex(
        for mode: AnswerMode,
        currentChapterIndex: Int
    ) -> Int
}

public protocol ResponseSpoilerScanning: Sendable {
    func containsObviousSpoiler(
        _ answer: String,
        currentChapterIndex: Int,
        knownEntities: [String]
    ) -> Bool
}
