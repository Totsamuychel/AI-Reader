import Foundation
import SharedModels

public final class PromptContextAssembler: PromptContextAssembling {
    public init() {}

    public func makePromptContext(
        from retrievalContext: RetrievalContext,
        question: String,
        currentChapterIndex: Int,
        spoilerBoundary: Int
    ) -> String {
        var out = ""
        out += "## Reading state\n"
        out += "- Current chapter index: \(currentChapterIndex)\n"
        out += "- Allowed knowledge horizon (inclusive chapter index): \(spoilerBoundary)\n"
        out += "- Anything not in the context below MUST be treated as unknown.\n\n"

        if let recap = retrievalContext.safeRecap {
            out += "## Safe recap (chapter \(recap.chapterIndex))\n\(recap.recapText)\n\n"
        }

        if !retrievalContext.characterCards.isEmpty {
            out += "## Character cards\n"
            for c in retrievalContext.characterCards.prefix(6) {
                var line = "- \(c.canonicalName)"
                if !c.aliases.isEmpty { line += " (aka \(c.aliases.joined(separator: ", ")))" }
                line += " — first seen ch.\(c.firstChapterIndex), known up to ch.\(c.lastSafeChapterIndex)"
                if let summary = c.safeSummary, !summary.isEmpty { line += ": \(summary)" }
                out += line + "\n"
            }
            out += "\n"
        }

        if !retrievalContext.recentEvents.isEmpty {
            out += "## Recent events (no spoilers past ch.\(spoilerBoundary))\n"
            for e in retrievalContext.recentEvents.prefix(5) {
                out += "- ch.\(e.chapterIndex) — \(e.shortDescription)\n"
            }
            out += "\n"
        }

        if !retrievalContext.facts.isEmpty {
            out += "## Facts\n"
            for f in retrievalContext.facts.prefix(8) {
                out += "- ch.\(f.chapterIndex) [\(f.factType)] \(f.text)\n"
            }
            out += "\n"
        }

        if !retrievalContext.quotes.isEmpty {
            out += "## Relevant quotes\n"
            for q in retrievalContext.quotes.prefix(3) {
                let preview = q.text.prefix(280)
                out += "> \(preview)\n"
            }
            out += "\n"
        }

        out += "## User question\n\(question)\n"
        return out
    }
}
