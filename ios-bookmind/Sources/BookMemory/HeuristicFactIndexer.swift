import Foundation
import SharedModels

/// Produces basic facts: appearance of named characters and quoted dialog lines.
public final class HeuristicFactIndexer: FactIndexing {
    private let characterDetector: HeuristicCharacterDetector
    public init(characterDetector: HeuristicCharacterDetector = HeuristicCharacterDetector()) {
        self.characterDetector = characterDetector
    }

    public func extractFacts(from chapter: Chapter, rawText: String) -> [Fact] {
        var facts: [Fact] = []
        let names = characterDetector.extractCandidateNames(from: rawText)
        for name in names {
            facts.append(Fact(
                id: "\(chapter.id.rawValue)#appear-\(name.lowercased())",
                bookID: chapter.bookID,
                chapterID: chapter.id,
                chapterIndex: chapter.index,
                factType: "character_appearance",
                subjectID: name,
                objectID: nil,
                text: "Персонаж \(name) упомянут в главе \(chapter.index + 1).",
                spoilerLevel: 0
            ))
        }

        // First two quoted lines as light dialog snippets.
        let quotePattern = #"[«"]([^«»"]{8,200})[»"]"#
        if let regex = try? NSRegularExpression(pattern: quotePattern) {
            let ns = rawText as NSString
            let matches = regex.matches(in: rawText, range: NSRange(location: 0, length: ns.length))
            for (i, m) in matches.prefix(2).enumerated() where m.numberOfRanges >= 2 {
                let quote = ns.substring(with: m.range(at: 1))
                facts.append(Fact(
                    id: "\(chapter.id.rawValue)#quote-\(i)",
                    bookID: chapter.bookID,
                    chapterID: chapter.id,
                    chapterIndex: chapter.index,
                    factType: "dialog",
                    subjectID: nil,
                    objectID: nil,
                    text: quote,
                    spoilerLevel: 1
                ))
            }
        }
        return facts
    }
}
