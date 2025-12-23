package io.github.swiftstagrime.termuxrunner.ui.features.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class SearchVisualTransformation(
    private val matches: List<IntRange>,
    private val activeMatchIndex: Int,
    private val activeColor: Color,
    private val passiveColor: Color
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)

        for ((index, range) in matches.withIndex()) {
            if (range.last < text.length) {
                val color = if (index == activeMatchIndex) activeColor else passiveColor
                builder.addStyle(
                    style = SpanStyle(background = color),
                    start = range.first,
                    end = range.last + 1
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}