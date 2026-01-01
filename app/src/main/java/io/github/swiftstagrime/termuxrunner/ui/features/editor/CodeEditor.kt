package io.github.swiftstagrime.termuxrunner.ui.features.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.util.LanguageUtils
import io.github.swiftstagrime.termuxrunner.ui.extensions.insert
import io.github.swiftstagrime.termuxrunner.ui.extensions.toggleComment
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CodeEditor(
    code: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    interpreter: String,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    val undoStack = remember { ArrayDeque<TextFieldValue>().apply { add(code) } }
    val redoStack = remember { ArrayDeque<TextFieldValue>() }
    var lastUndoSave by remember { mutableStateOf(code) }

    var isSearchVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var currentMatchIndex by remember { mutableIntStateOf(-1) }

    var isAccessoryVisible by remember { mutableStateOf(true) }
    val toolbarHeight = 50.dp
    val extraLines = 5
    val bottomBuffer = toolbarHeight + with(LocalDensity.current) { (extraLines * 20).sp.toDp() }

    var isWrappingEnabled by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val currentCode by rememberUpdatedState(code)
    val currentOnCodeChange by rememberUpdatedState(onCodeChange)

    fun getIndentationForNewLine(text: String, cursorIndex: Int): String {
        if (cursorIndex <= 0) return ""
        val textBeforeCursor = text.take(cursorIndex)
        val lastNewLineIndex = textBeforeCursor.lastIndexOf('\n')
        val lineStart = if (lastNewLineIndex == -1) 0 else lastNewLineIndex + 1
        val currentLinePrefix = textBeforeCursor.substring(lineStart)
        return currentLinePrefix.takeWhile { it.isWhitespace() }
    }

    fun handleCodeChange(newValue: TextFieldValue) {
        val oldText = currentCode.text
        val newText = newValue.text
        val cursor = newValue.selection.start

        if (newText.length == oldText.length + 1) {
            val charInserted = newText[cursor - 1]

            if (charInserted == '\n') {
                val indentation = getIndentationForNewLine(newText, cursor - 1)

                if (indentation.isNotEmpty()) {
                    val textWithIndent =
                        newText.take(cursor) + indentation + newText.substring(cursor)
                    val newCursor = cursor + indentation.length

                    val indentedValue = newValue.copy(
                        text = textWithIndent,
                        selection = TextRange(newCursor)
                    )
                    currentOnCodeChange(indentedValue)
                    return
                }
            }

            val nextChar = if (cursor < newText.length) newText[cursor] else null
            val pairs = mapOf('(' to ')', '{' to '}', '[' to ']', '"' to '"', '\'' to '\'')

            if (pairs.containsKey(charInserted)) {
                val closing = pairs[charInserted]
                currentOnCodeChange(
                    newValue.insert(closing.toString())
                        .copy(selection = TextRange(cursor))
                )
                return
            } else if (pairs.containsValue(charInserted) && nextChar == charInserted) {
                currentOnCodeChange(currentCode.copy(selection = TextRange(cursor + 1)))
                return
            }
        }

        currentOnCodeChange(newValue)
    }

    val handleInsertSymbol = remember {
        { symbol: String ->
            val textValue = currentCode
            when (symbol) {
                "HOME_KEY" -> {
                    val lineStart =
                        textValue.text.lastIndexOf('\n', textValue.selection.start - 1) + 1
                    currentOnCodeChange(textValue.copy(selection = TextRange(lineStart)))
                }

                "END_KEY" -> {
                    val lineEnd = textValue.text.indexOf('\n', textValue.selection.start)
                        .let { if (it == -1) textValue.text.length else it }
                    currentOnCodeChange(textValue.copy(selection = TextRange(lineEnd)))
                }

                "\n" -> {
                    val indent = getIndentationForNewLine(textValue.text, textValue.selection.start)
                    currentOnCodeChange(textValue.insert("\n$indent"))
                }

                "BACKTAB" -> {
                    val cursor = textValue.selection.start
                    val textStr = textValue.text
                    val lineStart =
                        textStr.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }

                    val fourSpaces = "    "
                    val tab = "\t"

                    val newTextValue = if (textStr.startsWith(fourSpaces, lineStart)) {
                        val newText = textStr.removeRange(lineStart, lineStart + 4)
                        val newCursor = (cursor - 4).coerceAtLeast(lineStart)
                        textValue.copy(text = newText, selection = TextRange(newCursor))
                    } else if (textStr.startsWith(tab, lineStart)) {
                        val newText = textStr.removeRange(lineStart, lineStart + 1)
                        val newCursor = (cursor - 1).coerceAtLeast(lineStart)
                        textValue.copy(text = newText, selection = TextRange(newCursor))
                    } else {
                        textValue
                    }
                    currentOnCodeChange(newTextValue)
                }

                else -> {
                    currentOnCodeChange(textValue.insert(symbol))
                }
            }
        }
    }

    val handleToggleComment = remember {
        {
            val symbol = LanguageUtils.getCommentSymbol(interpreter)
            currentOnCodeChange(currentCode.toggleComment(symbol))
        }
    }

    val searchMatches by remember(code.text, searchQuery) {
        derivedStateOf {
            if (searchQuery.isEmpty()) return@derivedStateOf emptyList()
            val matches = mutableListOf<IntRange>()
            var index = code.text.indexOf(searchQuery, ignoreCase = true)
            while (index >= 0) {
                matches.add(index until (index + searchQuery.length))
                index = code.text.indexOf(searchQuery, index + 1, ignoreCase = true)
            }
            matches
        }
    }

    LaunchedEffect(searchQuery) {
        currentMatchIndex = if (searchMatches.isNotEmpty()) 0 else -1
    }

    fun navigateSearch(direction: Int) {
        if (searchMatches.isEmpty()) return
        var newIndex = currentMatchIndex + direction
        if (newIndex >= searchMatches.size) newIndex = 0
        if (newIndex < 0) newIndex = searchMatches.size - 1

        currentMatchIndex = newIndex
        val range = searchMatches[newIndex]
        onCodeChange(code.copy(selection = TextRange(range.first, range.last + 1)))
    }

    val activeMatchColor = MaterialTheme.colorScheme.primaryContainer
    val passiveMatchColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    val searchTransformation = remember(searchMatches, currentMatchIndex, isSearchVisible) {
        if (isSearchVisible && searchMatches.isNotEmpty()) {
            SearchVisualTransformation(
                matches = searchMatches,
                activeMatchIndex = currentMatchIndex,
                activeColor = activeMatchColor,
                passiveColor = passiveMatchColor
            )
        } else {
            VisualTransformation.None
        }
    }

    LaunchedEffect(currentMatchIndex) {
        if (currentMatchIndex != -1 && textLayoutResult != null) {
            val matchRange = searchMatches[currentMatchIndex]
            val line = textLayoutResult!!.getLineForOffset(matchRange.first)
            val yOffset = textLayoutResult!!.getLineTop(line).toInt()
            verticalScrollState.animateScrollTo(yOffset)
        }
    }


    fun addToUndo(newValue: TextFieldValue) {
        if (undoStack.isEmpty() || undoStack.last().text != newValue.text) {
            undoStack.addLast(newValue)
            if (undoStack.size > 30) undoStack.removeFirst()
            redoStack.clear()
        }
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeLast()
            redoStack.addLast(current)
            onCodeChange(undoStack.last())
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeLast()
            undoStack.addLast(next)
            onCodeChange(next)
        }
    }

    LaunchedEffect(code.text) {
        delay(1000)
        if (lastUndoSave.text != code.text) {
            addToUndo(code)
            lastUndoSave = code
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            textStyle = TextStyle(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            ),
                            singleLine = true,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (searchQuery.isEmpty()) Text(
                                    stringResource(R.string.editor_search_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                inner()
                            }
                        )
                        if (searchMatches.isNotEmpty()) {
                            Text(
                                text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                        IconButton(
                            onClick = { navigateSearch(-1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                stringResource(R.string.cd_prev_match),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { navigateSearch(1) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                stringResource(R.string.cd_next_match),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = { isSearchVisible = false; searchQuery = "" },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                stringResource(R.string.cd_close_search),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                LineNumberGutter(
                    text = code.text,
                    layoutResult = textLayoutResult,
                    scrollState = verticalScrollState,
                    extraLines = extraLines,
                    bottomBuffer = bottomBuffer // Pass the buffer here
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(verticalScrollState)
                        .then(
                            if (isWrappingEnabled) Modifier
                            else Modifier.horizontalScroll(rememberScrollState())
                        )
                ) {
                    BasicTextField(
                        value = code,
                        onValueChange = { handleCodeChange(it) },
                        onTextLayout = { textLayoutResult = it },
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        visualTransformation = searchTransformation,
                        modifier = Modifier
                            .then(if (isWrappingEnabled) Modifier.fillMaxWidth() else Modifier)
                            .padding(horizontal = 8.dp)
                            .focusRequester(focusRequester),
                        decorationBox = { inner ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = toolbarHeight + 100.dp)
                            ) {
                                Box {
                                    if (code.text.isEmpty()) {
                                        Text(
                                            stringResource(R.string.editor_placeholder),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = 0.5f
                                            )
                                        )
                                    }
                                    inner()
                                }

                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(bottomBuffer)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            onCodeChange(code.copy(selection = TextRange(code.text.length)))
                                            focusRequester.requestFocus()
                                        }
                                )
                            }
                        }
                    )
                }
            }
        }

        val animationDuration = 300
        AnimatedContent(
            targetState = isAccessoryVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            label = "ToolbarTransition",
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(animationSpec = tween(animationDuration)) { height -> height } +
                            fadeIn(animationSpec = tween(animationDuration)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(animationDuration))
                        )
                } else {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = animationDuration
                        )
                    )
                        .togetherWith(
                            slideOutVertically(animationSpec = tween(animationDuration)) { height -> height } +
                                    fadeOut(animationSpec = tween(animationDuration))
                        )
                }
            }
        ) { showToolbar ->

            if (showToolbar) {
                EditorAccessoryToolbar(
                    undoEnabled = undoStack.size > 1,
                    redoEnabled = redoStack.isNotEmpty(),
                    onUndo = { undo() },
                    onRedo = { redo() },
                    onToggleSearch = { isSearchVisible = !isSearchVisible },
                    onToggleComment = handleToggleComment,
                    onHide = { isAccessoryVisible = false },
                    onInsertSymbol = handleInsertSymbol,
                    interpreter = interpreter,
                    onToggleWrap = { isWrappingEnabled = !isWrappingEnabled },
                    isWrappingEnabled = isWrappingEnabled,
                    onScrollTop = {
                        coroutineScope.launch { verticalScrollState.animateScrollTo(0) }
                    },
                    onScrollBottom = {
                        coroutineScope.launch { verticalScrollState.animateScrollTo(verticalScrollState.maxValue) }
                    }
                )
            } else {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAccessoryVisible = true }
                ) {
                    Box(
                        modifier = Modifier.height(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            stringResource(R.string.cd_show_toolbar),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccessoryKey(
    symbol: String,
    width: Dp = 36.dp,
    highlight: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .widthIn(min = width)
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            ),
            color = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun LineNumberGutter(
    text: String,
    layoutResult: TextLayoutResult?,
    scrollState: ScrollState,
    extraLines: Int,
    bottomBuffer: Dp,
    modifier: Modifier = Modifier
) {
    val fontSize = 14.sp
    val lineHeight = 20.sp

    Column(
        modifier = modifier
            .width(48.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.End
    ) {
        if (layoutResult != null) {
            val lineStarts = remember(text) {
                val starts = mutableSetOf<Int>()
                starts.add(0)
                text.forEachIndexed { index, char ->
                    if (char == '\n') starts.add(index + 1)
                }
                starts
            }

            var lastDrawnLine = -1
            for (i in 0 until layoutResult.lineCount) {
                val startOffset = layoutResult.getLineStart(i)
                val isNewPhysicalLine = lineStarts.contains(startOffset)

                Box(modifier = Modifier.height(with(LocalDensity.current) {
                    (layoutResult.getLineBottom(i) - layoutResult.getLineTop(i)).toDp()
                })) {
                    if (isNewPhysicalLine) {
                        lastDrawnLine++
                        Text(
                            text = (lastDrawnLine + 1).toString(),
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = fontSize,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End
                            ),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            val currentPhysicalLineCount = text.count { it == '\n' } + 1
            repeat(extraLines) { i ->
                Text(
                    text = (currentPhysicalLineCount + i + 1).toString(),
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = fontSize,
                        lineHeight = lineHeight,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(with(LocalDensity.current) { lineHeight.toDp() })
                        .padding(horizontal = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(bottomBuffer))
    }
}

@Composable
private fun EditorAccessoryToolbar(
    undoEnabled: Boolean,
    redoEnabled: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleSearch: () -> Unit,
    onToggleComment: () -> Unit,
    onHide: () -> Unit,
    onInsertSymbol: (String) -> Unit,
    onToggleWrap: () -> Unit,
    onScrollTop: () -> Unit,    
    onScrollBottom: () -> Unit,
    isWrappingEnabled: Boolean,
    interpreter: String
) {
    val snippets = remember(interpreter) {
        getSnippetsForInterpreter(interpreter)
    }

    val symbols = remember {
        listOf(
            "(",
            ")",
            "{",
            "}",
            "[",
            "]",
            "\"",
            "'",
            "=",
            "$",
            "|",
            "&",
            ";",
            "/",
            "\\",
            "!",
            "<",
            ">"
        )
    }

    Column {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row {
                        IconButton(onClick = onUndo, enabled = undoEnabled) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                stringResource(R.string.cd_undo),
                                tint = if (undoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.3f
                                )
                            )
                        }
                        IconButton(onClick = onRedo, enabled = redoEnabled) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                stringResource(R.string.cd_redo),
                                tint = if (redoEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(
                                    alpha = 0.3f
                                )
                            )
                        }
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                Icons.Default.Search,
                                stringResource(R.string.cd_search),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onScrollTop) {
                            Icon(Icons.Default.VerticalAlignTop, "Top", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = onScrollBottom) {
                            Icon(Icons.Default.VerticalAlignBottom, "Bottom", tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = onToggleComment) {
                            Icon(
                                Icons.AutoMirrored.Filled.Comment,
                                stringResource(R.string.cd_comment),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = onToggleWrap) {
                            Icon(
                                if (isWrappingEnabled) Icons.AutoMirrored.Filled.WrapText else Icons.AutoMirrored.Filled.FormatAlignLeft,
                                contentDescription = "Toggle Wrap",
                                tint = if (isWrappingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    IconButton(onClick = onHide) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            stringResource(R.string.cd_hide_toolbar),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyRow(
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        AccessoryKey(
                            "⏎",
                            width = 48.dp,
                            highlight = true
                        ) { onInsertSymbol("\n") }
                    }
                    item { AccessoryKey("⇤", width = 40.dp) { onInsertSymbol("BACKTAB") } }
                    item { AccessoryKey("⇥", width = 40.dp) { onInsertSymbol("    ") } }

                    item { AccessoryKey("Home", width = 50.dp) { onInsertSymbol("HOME_KEY") } }
                    item { AccessoryKey("End", width = 50.dp) { onInsertSymbol("END_KEY") } }

                    items(snippets) { snippet ->
                        AccessoryKey(snippet.label, width = 60.dp) {
                            onInsertSymbol(snippet.code)
                        }
                    }

                    items(symbols) { symbol ->
                        AccessoryKey(symbol) { onInsertSymbol(symbol) }
                    }
                }
            }
        }
    }
}

@DevicePreviews
@Composable
fun CodeEditorPreview() {
    val initialText = """
def calculate_sum(a, b):
    result = a + b
    return result

# TODO: Add more functions
    """.trimIndent()

    var codeState by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            CodeEditor(
                code = codeState,
                onCodeChange = { codeState = it },
                interpreter = "python",
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

data class Snippet(val label: String, val code: String)

fun getSnippetsForInterpreter(interpreter: String): List<Snippet> {
    return when (interpreter.trim()) {
        "python", "python3" -> listOf(
            Snippet("def", "def name():\n    "),
            Snippet("if", "if condition:\n    "),
            Snippet("print", "print()")
        )

        "node", "nodejs", "js" -> listOf(
            Snippet("func", "function name() {\n    \n}"),
            Snippet("log", "console.log()"),
            Snippet("if", "if (condition) {\n    \n}")
        )

        "bash", "sh" -> listOf(
            Snippet("if", "if [ condition ]; then\n    \nfi"),
            Snippet("echo", "echo \"\""),
            Snippet("var", "VAR=\"value\"")
        )

        "cpp", "g++", "gcc" -> listOf(
            Snippet("main", "int main() {\n    return 0;\n}"),
            Snippet("inc", "#include <iostream>"),
            Snippet("out", "std::cout <<  << std::endl;")
        )

        else -> emptyList()
    }
}

