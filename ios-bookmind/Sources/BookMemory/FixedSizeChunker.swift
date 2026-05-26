import Foundation
import SharedModels

/// Splits chapter text into roughly fixed-size chunks at sentence boundaries.
public final class FixedSizeChunker: ChapterChunking {
    public let targetTokenCount: Int
    public let overlapTokenCount: Int

    public init(targetTokenCount: Int = 350, overlapTokenCount: Int = 40) {
        self.targetTokenCount = targetTokenCount
        self.overlapTokenCount = overlapTokenCount
    }

    public func makeChunks(for chapter: Chapter, rawText: String) -> [Chunk] {
        let sentences = splitSentences(rawText)
        guard !sentences.isEmpty else { return [] }

        var chunks: [Chunk] = []
        var current: [String] = []
        var currentTokens = 0
        var chunkIdx = 0

        for sentence in sentences {
            let tokens = approxTokenCount(for: sentence)
            if currentTokens + tokens > targetTokenCount, !current.isEmpty {
                chunks.append(makeChunk(
                    chapter: chapter,
                    idx: chunkIdx,
                    sentences: current,
                    tokens: currentTokens
                ))
                chunkIdx += 1
                // overlap: keep last sentences worth ~overlapTokenCount
                var overlap: [String] = []
                var overlapTokens = 0
                for s in current.reversed() {
                    overlap.insert(s, at: 0)
                    overlapTokens += approxTokenCount(for: s)
                    if overlapTokens >= overlapTokenCount { break }
                }
                current = overlap
                currentTokens = overlapTokens
            }
            current.append(sentence)
            currentTokens += tokens
        }
        if !current.isEmpty {
            chunks.append(makeChunk(
                chapter: chapter,
                idx: chunkIdx,
                sentences: current,
                tokens: currentTokens
            ))
        }
        return chunks
    }

    private func makeChunk(chapter: Chapter, idx: Int, sentences: [String], tokens: Int) -> Chunk {
        Chunk(
            id: ChunkID("\(chapter.id.rawValue)#chunk-\(idx)"),
            bookID: chapter.bookID,
            chapterID: chapter.id,
            index: idx,
            text: sentences.joined(separator: " ").trimmingCharacters(in: .whitespacesAndNewlines),
            tokenCount: tokens,
            spoilerLevel: 0
        )
    }

    private func splitSentences(_ text: String) -> [String] {
        // Locale-aware sentence boundary tokenizer.
        var result: [String] = []
        let tokenizer = NSLinguisticTagger(tagSchemes: [.tokenType], options: 0)
        tokenizer.string = text
        tokenizer.enumerateTags(
            in: NSRange(location: 0, length: (text as NSString).length),
            unit: .sentence,
            scheme: .tokenType,
            options: []
        ) { _, range, _ in
            let s = (text as NSString).substring(with: range)
                .trimmingCharacters(in: .whitespacesAndNewlines)
            if !s.isEmpty { result.append(s) }
        }
        if result.isEmpty {
            // Fallback for tiny inputs
            return text.split(separator: ".", omittingEmptySubsequences: true)
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) + "." }
        }
        return result
    }

    private func approxTokenCount(for text: String) -> Int {
        // Rough heuristic: one token ≈ 4 chars (English) / a word in Cyrillic.
        max(1, text.count / 4)
    }
}
