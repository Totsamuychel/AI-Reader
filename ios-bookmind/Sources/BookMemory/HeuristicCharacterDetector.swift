import Foundation
import SharedModels

/// Detects characters by extracting proper nouns / capitalized name-like tokens.
/// Naive but adequate as a first pass; replace with NER later.
public final class HeuristicCharacterDetector: CharacterDetecting {
    public init() {}

    private static let stopWords: Set<String> = [
        "The", "A", "An", "He", "She", "It", "They", "We", "I", "You",
        "Chapter", "Part", "Book", "Mr", "Mrs", "Ms", "Dr",
        "Глава", "Часть", "Книга", "Господин", "Госпожа"
    ]

    public func detectCharacters(
        in chapter: Chapter,
        rawText: String,
        existing: [Character]
    ) -> [Character] {
        let names = extractCandidateNames(from: rawText)
        var byCanonical: [String: Character] = [:]
        for c in existing { byCanonical[c.canonicalName.lowercased()] = c }

        var result: [Character] = []
        for name in names {
            let key = name.lowercased()
            if let prior = byCanonical[key] {
                // Extend last safe chapter index up to current chapter.
                if chapter.index > prior.lastSafeChapterIndex {
                    let updated = Character(
                        id: prior.id,
                        bookID: prior.bookID,
                        canonicalName: prior.canonicalName,
                        aliases: prior.aliases,
                        firstChapterIndex: prior.firstChapterIndex,
                        lastSafeChapterIndex: chapter.index,
                        safeSummary: prior.safeSummary
                    )
                    byCanonical[key] = updated
                    result.append(updated)
                }
            } else {
                let new = Character(
                    id: "\(chapter.bookID.rawValue)#char-\(stableSlug(name))",
                    bookID: chapter.bookID,
                    canonicalName: name,
                    aliases: [],
                    firstChapterIndex: chapter.index,
                    lastSafeChapterIndex: chapter.index,
                    safeSummary: nil
                )
                byCanonical[key] = new
                result.append(new)
            }
        }
        return result
    }

    func extractCandidateNames(from text: String) -> [String] {
        // Words starting with an uppercase letter, appearing >=2 times, length 2-30.
        let pattern = #"\b\p{Lu}\p{L}{1,29}\b"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
        let ns = text as NSString
        let matches = regex.matches(in: text, range: NSRange(location: 0, length: ns.length))

        var freq: [String: Int] = [:]
        for m in matches {
            let token = ns.substring(with: m.range)
            if Self.stopWords.contains(token) { continue }
            freq[token, default: 0] += 1
        }
        // Filter: appears at least twice OR clearly a name structure (length >= 3).
        return freq
            .filter { $0.value >= 2 && $0.key.count >= 3 }
            .map(\.key)
            .sorted()
    }

    private func stableSlug(_ s: String) -> String {
        s.lowercased()
            .replacingOccurrences(of: #"[^\p{L}\p{N}]+"#, with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))
    }
}
