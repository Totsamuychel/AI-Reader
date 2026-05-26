import Foundation
import SharedModels

/// Builds a non-spoiler recap by taking the leading sentences of the chapter
/// and trimming to a fixed budget. Replace with LLM-based summarization later.
public final class HeuristicRecapBuilder: SafeRecapBuilding {
    public let maxSentences: Int
    public let maxCharacters: Int

    public init(maxSentences: Int = 4, maxCharacters: Int = 600) {
        self.maxSentences = maxSentences
        self.maxCharacters = maxCharacters
    }

    public func buildRecap(for chapter: Chapter, chunks: [Chunk]) -> Recap {
        let joined = chunks.prefix(2).map(\.text).joined(separator: " ")
        let firstSentences = leadingSentences(of: joined, count: maxSentences)
        var text = firstSentences.joined(separator: " ")
        if text.count > maxCharacters {
            let idx = text.index(text.startIndex, offsetBy: maxCharacters)
            text = String(text[..<idx]) + "…"
        }
        return Recap(
            id: "\(chapter.id.rawValue)#recap",
            bookID: chapter.bookID,
            chapterID: chapter.id,
            chapterIndex: chapter.index,
            recapText: text,
            style: "safe"
        )
    }

    private func leadingSentences(of text: String, count: Int) -> [String] {
        var result: [String] = []
        let tokenizer = NSLinguisticTagger(tagSchemes: [.tokenType], options: 0)
        tokenizer.string = text
        tokenizer.enumerateTags(
            in: NSRange(location: 0, length: (text as NSString).length),
            unit: .sentence,
            scheme: .tokenType,
            options: []
        ) { _, range, stop in
            let s = (text as NSString).substring(with: range)
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if !s.isEmpty { result.append(s) }
            if result.count >= count { stop.pointee = true }
        }
        return result
    }
}
