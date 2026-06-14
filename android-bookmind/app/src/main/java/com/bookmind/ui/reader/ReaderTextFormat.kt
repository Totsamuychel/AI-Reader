package com.bookmind.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em

/**
 * Cleans raw chapter text extracted by the parsers into well-formed reading prose,
 * detecting paragraph structure across the two conventions books use:
 *
 *  - **Blank-line separated** (most EPUB/FB2/PDF, and TXT with empty lines): blank
 *    lines delimit paragraphs and any single newline inside a paragraph is a
 *    hard-wrap that we unwrap into a space.
 *  - **One-line-per-paragraph** (common plain TXT with no blank lines): every
 *    non-empty line is its own paragraph.
 *
 * Paragraphs are emitted separated by a single `\n` so the reader can render them
 * with a book-style first-line indent (see [ReaderVisualTransformation]); [paginate]
 * splits on the same delimiter.
 */
fun normalizeChapterText(raw: String): String {
    if (raw.isBlank()) return ""
    val text = raw.replace("\r\n", "\n").replace(' ', ' ') // nbsp → space
    val hasBlankLineSeparators = Regex("\\n[ \\t]*\\n").containsMatchIn(text)

    val paragraphs = if (hasBlankLineSeparators) {
        text.split(Regex("\\n[ \\t]*\\n"))
            .map { it.replace(Regex("[ \\t]*\\n[ \\t]*"), " ") } // unwrap hard-wraps
    } else {
        text.split('\n') // each line is a paragraph
    }

    return paragraphs
        .asSequence()
        .map { it.replace(Regex("[ \\t]{2,}"), " ").trim() } // collapse repeated spaces
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

/**
 * Renders the read-only reader text with book-like typography without changing
 * character offsets (so passage selection keeps working):
 *
 *  - every paragraph gets a first-line indent (the classic "red line")
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

        // Book-style first-line indent applied to every paragraph (split on '\n').
        builder.addStyle(
            ParagraphStyle(textIndent = TextIndent(firstLine = FIRST_LINE_INDENT)),
            0,
            source.length
        )

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
        // Relative to font size, so the indent scales with the reader's text size.
        val FIRST_LINE_INDENT = 1.6.em
    }
}
