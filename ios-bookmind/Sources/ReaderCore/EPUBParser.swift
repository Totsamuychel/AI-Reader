import Foundation
import SharedModels

/// Minimal EPUB parser. Reads container.xml -> OPF -> spine -> manifest items.
/// EPUB is a ZIP container; this implementation supports already-unpacked
/// directories *or* delegates extraction to the host app. For MVP we read
/// from an extracted folder (the import step expands the archive once).
public final class EPUBParser: BookParsing {
    private let unpacker: EPUBUnpacking

    public init(unpacker: EPUBUnpacking = ZIPFoundationUnpacker()) {
        self.unpacker = unpacker
    }

    public func parseChapters(for book: Book) async throws -> [Chapter] {
        let root = try await unpacker.ensureUnpacked(at: book.fileURL)
        let opfURL = try locateOPF(rootURL: root)
        let opfData = try Data(contentsOf: opfURL)
        let parsed = try OPFParser.parse(data: opfData)
        let opfDir = opfURL.deletingLastPathComponent()

        var chapters: [Chapter] = []
        for (idx, itemref) in parsed.spine.enumerated() {
            guard let item = parsed.manifest[itemref] else { continue }
            let contentURL = opfDir.appendingPathComponent(item.href)
            chapters.append(
                Chapter(
                    id: ChapterID("\(book.id.rawValue)#\(idx)"),
                    bookID: book.id,
                    index: idx,
                    title: item.title,
                    contentRef: contentURL
                )
            )
        }
        return chapters
    }

    public func rawText(for chapter: Chapter, in book: Book) async throws -> String {
        guard let url = chapter.contentRef else { return "" }
        let html = (try? String(contentsOf: url, encoding: .utf8)) ?? ""
        return HTMLStripper.plainText(from: html)
    }

    private func locateOPF(rootURL: URL) throws -> URL {
        let containerURL = rootURL
            .appendingPathComponent("META-INF")
            .appendingPathComponent("container.xml")
        guard let data = try? Data(contentsOf: containerURL),
              let xml = String(data: data, encoding: .utf8)
        else { throw ReaderError.parsingFailed("container.xml not found") }

        let pattern = #"full-path\s*=\s*"([^"]+)""#
        let regex = try NSRegularExpression(pattern: pattern)
        let ns = xml as NSString
        guard let match = regex.firstMatch(in: xml, range: NSRange(location: 0, length: ns.length)),
              match.numberOfRanges >= 2
        else { throw ReaderError.parsingFailed("OPF path not found in container.xml") }

        let path = ns.substring(with: match.range(at: 1))
        return rootURL.appendingPathComponent(path)
    }
}

// MARK: - Unpacker

public protocol EPUBUnpacking: Sendable {
    /// Returns a directory URL that contains the unpacked EPUB contents.
    func ensureUnpacked(at archiveURL: URL) async throws -> URL
}

/// Default unpacker — expects the archive already unpacked into a sibling
/// directory `<name>.epub.unpacked/` so we avoid mandatory third-party deps
/// in this scaffold. Replace with ZIPFoundation in the host app target.
public final class ZIPFoundationUnpacker: EPUBUnpacking {
    public init() {}

    public func ensureUnpacked(at archiveURL: URL) async throws -> URL {
        let fm = FileManager.default
        let extractedDir = archiveURL
            .deletingLastPathComponent()
            .appendingPathComponent(archiveURL.lastPathComponent + ".unpacked")

        if fm.fileExists(atPath: extractedDir.path) { return extractedDir }

        // If the URL itself is already a directory (e.g. exploded EPUB), use it.
        var isDir: ObjCBool = false
        if fm.fileExists(atPath: archiveURL.path, isDirectory: &isDir), isDir.boolValue {
            return archiveURL
        }

        throw ReaderError.parsingFailed(
            "EPUB unpacking requires ZIPFoundation integration in host app; provide an unpacked directory at \(extractedDir.path)"
        )
    }
}

// MARK: - OPF Parser

enum OPFParser {
    struct Item {
        let id: String
        let href: String
        let mediaType: String
        var title: String?
    }

    struct Parsed {
        let manifest: [String: Item]   // id -> Item
        let spine: [String]            // ordered idrefs
    }

    static func parse(data: Data) throws -> Parsed {
        let delegate = OPFXMLDelegate()
        let parser = XMLParser(data: data)
        parser.delegate = delegate
        guard parser.parse() else {
            throw ReaderError.parsingFailed("OPF XML invalid")
        }
        return Parsed(manifest: delegate.manifest, spine: delegate.spine)
    }
}

final class OPFXMLDelegate: NSObject, XMLParserDelegate {
    var manifest: [String: OPFParser.Item] = [:]
    var spine: [String] = []

    func parser(_ parser: XMLParser,
                didStartElement elementName: String,
                namespaceURI: String?,
                qualifiedName qName: String?,
                attributes attributeDict: [String: String] = [:]) {
        switch elementName.lowercased() {
        case "item":
            guard let id = attributeDict["id"],
                  let href = attributeDict["href"]
            else { return }
            let media = attributeDict["media-type"] ?? "application/xhtml+xml"
            manifest[id] = OPFParser.Item(id: id, href: href, mediaType: media, title: nil)
        case "itemref":
            if let idref = attributeDict["idref"] { spine.append(idref) }
        default: break
        }
    }
}

// MARK: - HTML stripper

enum HTMLStripper {
    static func plainText(from html: String) -> String {
        let withoutScripts = html.replacingOccurrences(
            of: #"<(script|style)[^>]*>[\s\S]*?</\1>"#,
            with: " ",
            options: .regularExpression
        )
        let withoutTags = withoutScripts.replacingOccurrences(
            of: "<[^>]+>", with: " ", options: .regularExpression
        )
        let entities = [
            "&nbsp;": " ", "&amp;": "&", "&lt;": "<", "&gt;": ">",
            "&quot;": "\"", "&#39;": "'", "&apos;": "'"
        ]
        var cleaned = withoutTags
        for (k, v) in entities { cleaned = cleaned.replacingOccurrences(of: k, with: v) }
        cleaned = cleaned.replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)
        return cleaned.trimmingCharacters(in: .whitespacesAndNewlines)
    }
}
