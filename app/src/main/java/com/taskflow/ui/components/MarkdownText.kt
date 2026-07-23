package com.taskflow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

/**
 * Renders a small, deliberate subset of markdown: # / ## / ### headers, **bold**, *italic*,
 * and "- " / "* " bullet lines. Everything else renders as plain text. This is not a full
 * CommonMark implementation — good enough for personal notes, not for arbitrary markdown files.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        markdown.lines().forEach { line ->
            when {
                line.startsWith("### ") -> Text(
                    text = parseInline(line.removePrefix("### ")),
                    style = MaterialTheme.typography.titleSmall
                )
                line.startsWith("## ") -> Text(
                    text = parseInline(line.removePrefix("## ")),
                    style = MaterialTheme.typography.titleMedium
                )
                line.startsWith("# ") -> Text(
                    text = parseInline(line.removePrefix("# ")),
                    style = MaterialTheme.typography.titleLarge
                )
                line.startsWith("- ") || line.startsWith("* ") -> Text(
                    text = buildAnnotatedString {
                        append("•  ")
                        append(parseInline(line.drop(2)))
                    }
                )
                line.isBlank() -> Text(text = "")
                else -> Text(text = parseInline(line))
            }
        }
    }
}

private val inlineTokenRegex = Regex("""\*\*(.+?)\*\*|\*(.+?)\*""")

/** Parses **bold** and *italic* spans out of a single line into an AnnotatedString. */
private fun parseInline(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in inlineTokenRegex.findAll(text)) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val bold = match.groups[1]?.value
        val italic = match.groups[2]?.value
        if (bold != null) {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
        } else if (italic != null) {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}