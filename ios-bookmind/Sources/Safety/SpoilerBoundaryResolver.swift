import Foundation

public final class SpoilerBoundaryResolver: SpoilerBoundaryResolving {
    public init() {}

    public func allowedChapterIndex(
        for mode: AnswerMode,
        currentChapterIndex: Int
    ) -> Int {
        switch mode {
        case .safe: return currentChapterIndex
        case .hint: return currentChapterIndex   // hints still bound by current chapter
        case .full: return .max
        }
    }
}

public final class HeuristicSpoilerScanner: ResponseSpoilerScanning {
    public init() {}

    // Phrases that often leak forward-looking events.
    private static let spoilerPhrases: [String] = [
        "in the end", "finally reveals", "turns out to be",
        "dies in", "kills", "betrays", "is the murderer",
        "marries", "the killer is",
        "в конце концов", "оказывается", "погибает", "предает",
        "убивает", "выходит замуж", "оказался"
    ]

    public func containsObviousSpoiler(
        _ answer: String,
        currentChapterIndex: Int,
        knownEntities: [String]
    ) -> Bool {
        let lowered = answer.lowercased()
        for phrase in Self.spoilerPhrases where lowered.contains(phrase) {
            return true
        }
        // Future-chapter references like "in chapter 14" / "в главе 14" with N > current.
        let pattern = #"(?:chapter|глав[аеы])\s*(\d{1,3})"#
        if let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) {
            let ns = answer as NSString
            for m in regex.matches(in: answer, range: NSRange(location: 0, length: ns.length))
            where m.numberOfRanges >= 2 {
                let n = Int(ns.substring(with: m.range(at: 1))) ?? 0
                if n > currentChapterIndex + 1 { return true }
            }
        }
        return false
    }
}
