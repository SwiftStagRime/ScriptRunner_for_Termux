package io.github.swiftstagrime.termuxrunner.ui.extensions

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun TextFieldValue.toggleComment(commentSymbol: String): TextFieldValue {
    val textStr = text
    val cursor = selection.start

    if (textStr.isEmpty()) return this

    val lineStart = textStr.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
    val lineEnd = textStr.indexOf('\n', cursor).let { if (it == -1) textStr.length else it }

    if (lineStart > textStr.length || lineEnd > textStr.length) return this

    val currentLine = textStr.substring(lineStart, lineEnd)

    val newLine = if (currentLine.trimStart().startsWith(commentSymbol)) {
        currentLine.replaceFirst(commentSymbol, "").replaceFirst(" ", "")
    } else {
        "$commentSymbol $currentLine"
    }

    val newText = textStr.replaceRange(lineStart, lineEnd, newLine)
    val diff = newLine.length - currentLine.length

    return copy(
        text = newText,
        selection = TextRange((cursor + diff).coerceIn(0, newText.length))
    )
}

fun TextFieldValue.insert(textToInsert: String): TextFieldValue {
    val start = selection.start
    val end = selection.end
    val newText = text.replaceRange(start, end, textToInsert)
    return copy(
        text = newText,
        selection = TextRange(start + textToInsert.length)
    )
}