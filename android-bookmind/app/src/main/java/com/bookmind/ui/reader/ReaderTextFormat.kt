package com.bookmind.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em

private val SENTENCE_ENDERS = charArrayOf('.', '!', '?', '…', '»', '”', '"', ')')

/** True when [line] looks like the end of a paragraph (ends a sentence). */
private fun endsSentence(line: String): Boolean {
    val c = line.trimEnd().lastOrNull() ?: return false
    return c in SENTENCE_ENDERS
}

/**
 * Cleans raw chapter text from the parsers into well-formed reading prose,
 * reconstructing paragraphs across the conventions books use:
 *
 *  - **Blank-line separated** (most EPUB/FB2): blank lines delimit paragraphs and
 *    single newlines inside are hard-wraps we unwrap into spaces.
 *  - **One-line-per-paragraph** TXT (no blank lines, most lines end a sentence):
 *    every line is its own paragraph.
 *  - **Hard-wrapped** text (PDF page extraction: many short lines, few end a
 *    sentence): lines are re-flowed into paragraphs, breaking only at a short
 *    sentence-ending line — so a PDF page no longer turns every wrapped line into
 *    its own indented paragraph.
 *
 * Paragraphs are emitted separated by a single `\n` (see [buildReaderAnnotatedString]
 * for the first-line indent); [paginate] splits on the same delimiter.
 */
fun normalizeChapterText(raw: String): String {
    if (raw.isBlank()) return ""
    val text = raw.replace("\r\n", "\n").replace(' ', ' ') // nbsp → space
    val hasBlankLineSeparators = Regex("\\n[ \\t]*\\n").containsMatchIn(text)

    val paragraphs: List<String> = if (hasBlankLineSeparators) {
        text.split(Regex("\\n[ \\t]*\\n"))
            .map { it.replace(Regex("[ \\t]*\\n[ \\t]*"), " ") } // unwrap hard-wraps
    } else {
        val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
        when {
            lines.size <= 1 -> lines
            // Mostly sentence-ending lines → genuine one-paragraph-per-line text.
            lines.count { endsSentence(it) }.toFloat() / lines.size >= 0.6f -> lines
            // Otherwise the text is hard-wrapped (e.g. PDF): re-flow into paragraphs.
            else -> reflowHardWrapped(lines)
        }
    }

    return paragraphs
        .asSequence()
        .map { it.replace(Regex("[ \\t]{2,}"), " ").trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n")
}

/**
 * Re-flows hard-wrapped lines into paragraphs: consecutive lines are joined with a
 * space, and a paragraph ends at a *short* sentence-ending line (the typical last
 * line of a paragraph), so full-width wrapped lines stay in one paragraph.
 */
private fun reflowHardWrapped(lines: List<String>): List<String> {
    val maxLen = lines.maxOf { it.length }.coerceAtLeast(1)
    val shortThreshold = maxLen * 0.75
    val paragraphs = mutableListOf<String>()
    val current = StringBuilder()
    for (line in lines) {
        if (current.isNotEmpty()) current.append(' ')
        current.append(line)
        if (endsSentence(line) && line.length < shortThreshold) {
            paragraphs.add(current.toString())
            current.clear()
        }
    }
    if (current.isNotEmpty()) paragraphs.add(current.toString())
    return paragraphs
}

// Relative to font size, so the indent scales with the reader's text size.
private val FIRST_LINE_INDENT = 1.6.em
private val INLINE_CODE = Regex("`[^`\\n]+`")

/**
 * Builds the styled reader text: a book-style first-line indent per paragraph,
 * italic+tinted block quotes (`>` lines) and monospace inline `` `code` ``. The
 * characters are unchanged from [source], so text-field offsets map 1:1.
 */
fun buildReaderAnnotatedString(source: String, quoteColor: Color): AnnotatedString {
    if (source.isEmpty()) return AnnotatedString("")
    return buildAnnotatedString {
        append(source)

        // First-line indent for every paragraph (split on '\n').
        addStyle(ParagraphStyle(textIndent = TextIndent(firstLine = FIRST_LINE_INDENT)), 0, source.length)

        // Quote lines: leading whitespace then '>'.
        var lineStart = 0
        while (lineStart <= source.length) {
            val newline = source.indexOf('\n', lineStart)
            val lineEnd = if (newline == -1) source.length else newline
            val firstNonSpace = (lineStart until lineEnd).firstOrNull { !source[it].isWhitespace() }
            if (firstNonSpace != null && source[firstNonSpace] == '>') {
                addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = quoteColor), lineStart, lineEnd)
            }
            if (newline == -1) break
            lineStart = newline + 1
        }

        // Inline code spans.
        for (match in INLINE_CODE.findAll(source)) {
            addStyle(SpanStyle(fontFamily = FontFamily.Monospace), match.range.first, match.range.last + 1)
        }
    }
}

/**
 * Visual transformation used by the selectable continuous reader (a read-only text
 * field). Reuses [buildReaderAnnotatedString] with [OffsetMapping.Identity].
 */
class ReaderVisualTransformation(
    private val quoteColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(buildReaderAnnotatedString(text.text, quoteColor), OffsetMapping.Identity)
}
