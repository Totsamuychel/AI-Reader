package com.bookmind.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Cleans raw chapter text extracted by the parsers into well-formed reading prose.
 *
 * Parsers emit paragraphs separated by blank lines, but the bytes around them are
 * inconsistent: stray single newlines mid-paragraph (hard-wrapped source), runs of
 * spaces, and leading/trailing whitespace. We collapse intra-paragraph whitespace to
 * single spaces while preserving the blank line between paragraphs, so the reader
 * renders clean blocks with consistent spacing across EPUB / FB2 / PDF / TXT.
 *
 * The result keeps `\n\n` as the only paragraph delimiter, which is exactly what
 * [paginate] splits on.
 */
fun normalizeChapterText(raw: String): String {
    if (raw.isBlank()) return ""
    return raw
        .replace("\r\n", "\n")
        .replace(' ', ' ') // non-breaking spaces → regular, so collapsing works
        .split(Regex("\\n\\s*\\n"))
        .asSequence()
        .map { paragraph ->
            paragraph
                .trim()
                .replace(Regex("[ \\t]*\\n[ \\t]*"), " ") // unwrap hard-wrapped lines
                .replace(Regex("[ \\t]{2,}"), " ")        // collapse repeated spaces
        }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

/**
 * Styles a few lightweight markup conventions inside the read-only reader text
 * field without changing character offsets (so passage selection keeps working):
 *
 *  - lines beginning with `>` render italic and tinted (block quotes)
 *  - inline `` `code` `` renders in a monospace family
 *
 * Because the visible text is identical to the source, [OffsetMapping.Identity] is
 * correct and selections map 1:1.
 */
class ReaderVisualTransformation(
    private val quoteColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val source = text.text
        if (source.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val builder = AnnotatedString.Builder(source)

        // Quote lines: leading whitespace then '>'.
        var lineStart = 0
        while (lineStart <= source.length) {
            val newline = source.indexOf('\n', lineStart)
            val lineEnd = if (newline == -1) source.length else newline
            val firstNonSpace = (lineStart until lineEnd).firstOrNull { !source[it].isWhitespace() }
            if (firstNonSpace != null && source[firstNonSpace] == '>') {
                builder.addStyle(
                    SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor),
                    lineStart,
                    lineEnd
                )
            }
            if (newline == -1) break
            lineStart = newline + 1
        }

        // Inline code spans.
        for (match in INLINE_CODE.findAll(source)) {
            builder.addStyle(
                SpanStyle(fontFamily = FontFamily.Monospace),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }

    private companion object {
        val INLINE_CODE = Regex("`[^`\\n]+`")
    }
}
