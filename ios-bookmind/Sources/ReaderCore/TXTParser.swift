import Foundation
import SharedModels

public final class TXTParser: BookParsing {
    public init() {}

    private static let chapterMarkers: [NSRegularExpression] = {
        let patterns = [
            #"^\s*Chapter\s+\d+.*$"#,
            #"^\s*CHAPTER\s+[IVXLC\d]+.*$"#,
            #"^\s*Глава\s+\d+.*$"#,
            #"^\s*ГЛАВА\s+[IVXLC\d]+.*$"#,
            #"^\s*\d+\.\s+\p{Lu}.*$"#
        ]
        return patterns.compactMap {
            try? NSRegularExpression(pattern: $0, options: [.anchorsMatchLines])
        }
    }()

    public func parseChapters(for book: Book) async throws -> [Chapter] {
        let text = try readText(at: book.fileURL)
        let splits = splitChapters(text: text)

        if splits.isEmpty {
            // Single-chapter fallback
            return [
                Chapter(
                    id: ChapterID("\(book.id.rawValue)#0"),
                    bookID: book.id,
                    index: 0,
                    title: book.title,
                    contentRef: book.fileURL
                )
            ]
        }

        return splits.enumerated().map { idx, slice in
            Chapter(
                id: ChapterID("\(book.id.rawValue)#\(idx)"),
                bookID: book.id,
                index: idx,
                title: slice.title,
                contentRef: book.fileURL
            )
        }
    }

    public func rawText(for chapter: Chapter, in book: Book) async throws -> String {
        let full = try readText(at: book.fileURL)
        let splits = splitChapters(text: full)
        if splits.isEmpty { return full }
        guard chapter.index >= 0, chapter.index < splits.count else { return "" }
        return splits[chapter.index].body
    }

    // MARK: - Internal

    struct ChapterSlice {
        let title: String?
        let body: String
    }

    func splitChapters(text: String) -> [ChapterSlice] {
        let nsText = text as NSString
        var matches: [(range: NSRange, title: String)] = []
        for regex in Self.chapterMarkers {
            let found = regex.matches(in: text, range: NSRange(location: 0, length: nsText.length))
            for m in found {
                matches.append((m.range, nsText.substring(with: m.range).trimmingCharacters(in: .whitespacesAndNewlines)))
            }
        }
        matches.sort { $0.range.location < $1.range.location }

        guard !matches.isEmpty else { return [] }

        var slices: [ChapterSlice] = []
        for (i, m) in matches.enumerated() {
            let bodyStart = m.range.location + m.range.length
            let bodyEnd = (i + 1 < matches.count) ? matches[i + 1].range.location : nsText.length
            guard bodyEnd > bodyStart else { continue }
            let body = nsText.substring(with: NSRange(location: bodyStart, length: bodyEnd - bodyStart))
                .trimmingCharacters(in: .whitespacesAndNewlines)
            slices.append(ChapterSlice(title: m.title, body: body))
        }
        return slices
    }

    private func readText(at url: URL) throws -> String {
        guard FileManager.default.isReadableFile(atPath: url.path) else {
            throw ReaderError.fileNotReadable(url)
        }
        let encodings: [String.Encoding] = [.utf8, .utf16, .isoLatin1, .windowsCP1251]
        for enc in encodings {
            if let s = try? String(contentsOf: url, encoding: enc) { return s }
        }
        throw ReaderError.parsingFailed("Could not decode text file")
    }
}
