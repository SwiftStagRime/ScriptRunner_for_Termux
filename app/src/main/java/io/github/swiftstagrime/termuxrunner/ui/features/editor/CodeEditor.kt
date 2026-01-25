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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material.icons.filled.VerticalAlignTop
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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.util.LanguageUtils
import io.github.swiftstagrime.termuxrunner.ui.extensions.insert
import io.github.swiftstagrime.termuxrunner.ui.extensions.toggleComment
import io.github.swiftstagrime.termuxrunner.ui.preview.DevicePreviews
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val UNDO_LIMIT = 30
private const val AUTO_SAVE_DELAY = 1000L
private const val ANIMATION_DURATION = 300
private const val LINE_HEIGHT_SP = 20
private const val EXTRA_LINES_COUNT = 5
private const val TOOLBAR_HEIGHT_DP = 50

val TextFieldValueSaver =
    listSaver<TextFieldValue, Any>(
        save = { listOf(it.text, it.selection.start, it.selection.end) },
        restore = {
            TextFieldValue(
                text = it[0] as String,
                selection = TextRange(it[1] as Int, it[2] as Int),
            )
        },
    )

val UndoStackSaver =
    listSaver<SnapshotStateList<TextFieldValue>, Any>(
        save = { it.map { tfv -> listOf(tfv.text, tfv.selection.start, tfv.selection.end) } },
        restore = { savedList ->
            val list = mutableStateListOf<TextFieldValue>()
            (savedList as List<List<Any>>).forEach { item ->
                list.add(
                    TextFieldValue(
                        text = item[0] as String,
                        selection = TextRange(item[1] as Int, item[2] as Int),
                    ),
                )
            }
            list
        },
    )

@Composable
fun CodeEditor(
    code: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    interpreter: String,
    modifier: Modifier = Modifier,
) {
    var isSearchVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var currentMatchIndex by rememberSaveable { mutableIntStateOf(-1) }
    var isAccessoryVisible by rememberSaveable { mutableStateOf(true) }
    var isWrappingEnabled by rememberSaveable { mutableStateOf(false) }

    val verticalScrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val undoStack = rememberSaveable(saver = UndoStackSaver) { mutableStateListOf(code) }
    val redoStack = rememberSaveable(saver = UndoStackSaver) { mutableStateListOf() }
    var lastUndoSave by rememberSaveable(stateSaver = TextFieldValueSaver) { mutableStateOf(code) }

    LaunchedEffect(code.text) {
        delay(AUTO_SAVE_DELAY)
        if (lastUndoSave.text != code.text) {
            undoStack.add(code)
            if (undoStack.size > UNDO_LIMIT) undoStack.removeAt(0)
            redoStack.clear()
            lastUndoSave = code
        }
    }

    val searchMatches by remember(code.text, searchQuery) {
        derivedStateOf { findSearchMatches(code.text, searchQuery) }
    }

    LaunchedEffect(searchQuery) {
        currentMatchIndex = if (searchMatches.isNotEmpty()) 0 else -1
    }

    val activeMatchColor = MaterialTheme.colorScheme.primaryContainer
    val passiveMatchColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)

    val searchTransformation =
        remember(searchMatches, currentMatchIndex, isSearchVisible, activeMatchColor, passiveMatchColor) {
            if (isSearchVisible && searchMatches.isNotEmpty()) {
                SearchVisualTransformation(searchMatches, currentMatchIndex, activeMatchColor, passiveMatchColor)
            } else {
                VisualTransformation.None
            }
        }

    val actions =
        remember(interpreter) {
            EditorActions(onCodeChange, interpreter, undoStack, redoStack)
        }

    Box(modifier = modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.fillMaxSize()) {
            EditorSearchBar(
                visible = isSearchVisible,
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                matchInfo = if (searchMatches.isNotEmpty()) "${currentMatchIndex + 1}/${searchMatches.size}" else null,
                onPrev = { currentMatchIndex = navigateSearch(searchMatches, currentMatchIndex, -1) },
                onNext = { currentMatchIndex = navigateSearch(searchMatches, currentMatchIndex, 1) },
                onClose = {
                    isSearchVisible = false
                    searchQuery = ""
                },
            )

            MainEditorArea(
                code = code,
                onCodeChange = { actions.handleCodeChange(code, it) },
                textLayoutResult = textLayoutResult,
                onTextLayout = { textLayoutResult = it },
                scrollState = verticalScrollState,
                isWrappingEnabled = isWrappingEnabled,
                visualTransformation = searchTransformation,
                focusRequester = focusRequester,
                onBottomClick = { actions.handleBottomClick(code, focusRequester) },
            )
        }

        EditorAccessoryWrapper(
            isVisible = isAccessoryVisible,
            onShow = { isAccessoryVisible = true },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            EditorAccessoryToolbar(
                undoEnabled = undoStack.size > 1,
                redoEnabled = redoStack.isNotEmpty(),
                onUndo = { actions.undo(onCodeChange) },
                onRedo = { actions.redo(onCodeChange) },
                onToggleSearch = { isSearchVisible = !isSearchVisible },
                onToggleComment = { onCodeChange(code.toggleComment(LanguageUtils.getCommentSymbol(interpreter))) },
                onHide = { isAccessoryVisible = false },
                onInsertSymbol = { actions.handleInsertSymbol(code, it) },
                interpreter = interpreter,
                onToggleWrap = { isWrappingEnabled = !isWrappingEnabled },
                isWrappingEnabled = isWrappingEnabled,
                onScrollTop = { coroutineScope.launch { verticalScrollState.animateScrollTo(0) } },
                onScrollBottom = { coroutineScope.launch { verticalScrollState.animateScrollTo(verticalScrollState.maxValue) } },
            )
        }
    }
}

@Composable
private fun EditorAccessoryWrapper(
    isVisible: Boolean,
    onShow: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    AnimatedContent(
        targetState = isVisible,
        modifier = modifier,
        label = "ToolbarTransition",
        transitionSpec = {
            val spec = tween<Float>(ANIMATION_DURATION)
            val slide = tween<IntOffset>(ANIMATION_DURATION)
            if (targetState) {
                (slideInVertically(slide) { it } + fadeIn(spec)).togetherWith(fadeOut(spec))
            } else {
                fadeIn(tween(ANIMATION_DURATION, delayMillis = ANIMATION_DURATION))
                    .togetherWith(slideOutVertically(slide) { it } + fadeOut(spec))
            }
        },
    ) { show ->
        if (show) {
            content()
        } else {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                modifier = Modifier.fillMaxWidth().clickable(onClick = onShow),
            ) {
                Box(modifier = Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.cd_show_toolbar), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

private class EditorActions(
    val onCodeChange: (TextFieldValue) -> Unit,
    val interpreter: String,
    val undoStack: MutableList<TextFieldValue>,
    val redoStack: MutableList<TextFieldValue>,
) {
    fun undo(update: (TextFieldValue) -> Unit) {
        if (undoStack.size > 1) {
            val current = undoStack.removeAt(undoStack.size - 1)
            redoStack.add(current)
            update(undoStack.last())
        }
    }

    fun redo(update: (TextFieldValue) -> Unit) {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.size - 1)
            undoStack.add(next)
            update(next)
        }
    }

    fun handleCodeChange(
        oldValue: TextFieldValue,
        newValue: TextFieldValue,
    ) {
        val newText = newValue.text
        val cursor = newValue.selection.start

        if (newText.length != oldValue.text.length + 1) {
            onCodeChange(newValue)
            return
        }

        val char = newText[cursor - 1]
        if (char == '\n') {
            val indent = getIndentation(newText, cursor - 1)
            if (indent.isNotEmpty()) {
                val result = newText.take(cursor) + indent + newText.substring(cursor)
                onCodeChange(newValue.copy(text = result, selection = TextRange(cursor + indent.length)))
                return
            }
        }

        val pairs = mapOf('(' to ')', '{' to '}', '[' to ']', '"' to '"', '\'' to '\'')
        if (pairs.containsKey(char)) {
            onCodeChange(newValue.insert(pairs[char].toString()).copy(selection = TextRange(cursor)))
            return
        }

        onCodeChange(newValue)
    }

    fun handleInsertSymbol(
        current: TextFieldValue,
        symbol: String,
    ) {
        when (symbol) {
            "HOME_KEY" -> {
                val start = current.text.lastIndexOf('\n', current.selection.start - 1) + 1
                onCodeChange(current.copy(selection = TextRange(start)))
            }
            "END_KEY" -> {
                val index = current.text.indexOf('\n', current.selection.start)
                val end = if (index == -1) current.text.length else index
                onCodeChange(current.copy(selection = TextRange(end)))
            }
            "BACKTAB" -> {
                val cursor = current.selection.start
                val lineStart = current.text.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
                val spaces = "    "
                if (current.text.startsWith(spaces, lineStart)) {
                    onCodeChange(
                        current.copy(
                            text = current.text.removeRange(lineStart, lineStart + 4),
                            selection = TextRange((cursor - 4).coerceAtLeast(lineStart)),
                        ),
                    )
                } else if (current.text.startsWith("\t", lineStart)) {
                    onCodeChange(
                        current.copy(
                            text = current.text.removeRange(lineStart, lineStart + 1),
                            selection = TextRange((cursor - 1).coerceAtLeast(lineStart)),
                        ),
                    )
                }
            }
            else -> onCodeChange(current.insert(symbol))
        }
    }

    fun handleBottomClick(
        code: TextFieldValue,
        focus: FocusRequester,
    ) {
        val currentText = code.text
        if (currentText.isNotEmpty() && !currentText.endsWith("\n")) {
            val indent = getIndentation(currentText, currentText.length)
            val newText = "$currentText\n$indent"
            onCodeChange(code.copy(text = newText, selection = TextRange(newText.length)))
        } else {
            onCodeChange(code.copy(selection = TextRange(currentText.length)))
        }
        focus.requestFocus()
    }
}

@Composable
private fun MainEditorArea(
    code: TextFieldValue,
    onCodeChange: (TextFieldValue) -> Unit,
    textLayoutResult: TextLayoutResult?,
    onTextLayout: (TextLayoutResult) -> Unit,
    scrollState: ScrollState,
    isWrappingEnabled: Boolean,
    visualTransformation: VisualTransformation,
    focusRequester: FocusRequester,
    onBottomClick: () -> Unit,
) {
    val bottomBuffer = with(LocalDensity.current) { (TOOLBAR_HEIGHT_DP.dp + (EXTRA_LINES_COUNT * LINE_HEIGHT_SP).sp.toDp() + 100.dp) }

    Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        LineNumberGutter(code.text, textLayoutResult, scrollState, EXTRA_LINES_COUNT, TOOLBAR_HEIGHT_DP.dp)

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(scrollState)
                    .then(if (isWrappingEnabled) Modifier else Modifier.horizontalScroll(rememberScrollState())),
        ) {
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                onTextLayout = onTextLayout,
                textStyle =
                    TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = LINE_HEIGHT_SP.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = visualTransformation,
                modifier =
                    Modifier
                        .then(if (isWrappingEnabled) Modifier.fillMaxWidth() else Modifier)
                        .padding(horizontal = 8.dp)
                        .focusRequester(focusRequester)
                        .testTag("code_editor_input"),
                decorationBox = { inner ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box {
                            if (code.text.isEmpty()) {
                                Text(
                                    stringResource(R.string.editor_placeholder),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                            inner()
                        }
                        Spacer(
                            modifier =
                                Modifier.fillMaxWidth().height(bottomBuffer).clickable(
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                    indication = null,
                                    onClick = onBottomClick,
                                ),
                        )
                    }
                },
            )
        }
    }
}

private fun getIndentation(
    text: String,
    cursor: Int,
): String {
    if (cursor <= 0) return ""
    val lastNL = text.take(cursor).lastIndexOf('\n')
    val start = if (lastNL == -1) 0 else lastNL + 1
    return text.substring(start, cursor).takeWhile { it.isWhitespace() }
}

private fun findSearchMatches(
    text: String,
    query: String,
): List<IntRange> {
    if (query.isEmpty()) return emptyList()
    val matches = mutableListOf<IntRange>()
    var index = text.indexOf(query, ignoreCase = true)
    while (index >= 0) {
        matches.add(index until (index + query.length))
        index = text.indexOf(query, index + 1, ignoreCase = true)
    }
    return matches
}

private fun navigateSearch(
    matches: List<IntRange>,
    current: Int,
    delta: Int,
): Int {
    if (matches.isEmpty()) return -1
    var next = current + delta
    if (next >= matches.size) next = 0
    if (next < 0) next = matches.size - 1
    return next
}

@Composable
fun EditorSearchBar(
    visible: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    matchInfo: String?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Search,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                    textStyle =
                        TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                        ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text(
                                stringResource(R.string.editor_search_placeholder),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                        inner()
                    },
                )
                if (matchInfo != null) {
                    Text(
                        text = matchInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        stringResource(R.string.cd_prev_match),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        stringResource(R.string.cd_next_match),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Close,
                        stringResource(R.string.cd_close_search),
                        tint = MaterialTheme.colorScheme.primary,
                    )
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
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(40.dp)
                .widthIn(min = width)
                .clip(RoundedCornerShape(8.dp))
                .background(if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style =
                MaterialTheme.typography.labelLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                ),
            color = if (highlight) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
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
    modifier: Modifier = Modifier,
) {
    val fontSize = 14.sp
    val lineHeight = 20.sp

    Column(
        modifier =
            modifier
                .width(48.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .verticalScroll(scrollState),
        horizontalAlignment = Alignment.End,
    ) {
        if (layoutResult != null) {
            val lineStarts =
                remember(text) {
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

                Box(
                    modifier =
                        Modifier.height(
                            with(LocalDensity.current) {
                                (layoutResult.getLineBottom(i) - layoutResult.getLineTop(i)).toDp()
                            },
                        ),
                ) {
                    if (isNewPhysicalLine) {
                        lastDrawnLine++
                        Text(
                            text = (lastDrawnLine + 1).toString(),
                            style =
                                TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSize,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.End,
                                ),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                        )
                    }
                }
            }

            val currentPhysicalLineCount = text.count { it == '\n' } + 1
            repeat(extraLines) { i ->
                Text(
                    text = (currentPhysicalLineCount + i + 1).toString(),
                    style =
                        TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSize,
                            lineHeight = lineHeight,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            textAlign = TextAlign.End,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(with(LocalDensity.current) { lineHeight.toDp() })
                            .padding(horizontal = 4.dp),
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
    interpreter: String,
) {
    val snippets =
        remember(interpreter) {
            getSnippetsForInterpreter(interpreter)
        }

    val symbols =
        remember {
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
                ">",
            )
        }

    Column {
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Column {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row {
                        IconButton(onClick = onUndo, enabled = undoEnabled) {
                            Icon(
                                Icons.AutoMirrored.Filled.Undo,
                                stringResource(R.string.cd_undo),
                                tint =
                                    if (undoEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.3f,
                                        )
                                    },
                            )
                        }
                        IconButton(onClick = onRedo, enabled = redoEnabled) {
                            Icon(
                                Icons.AutoMirrored.Filled.Redo,
                                stringResource(R.string.cd_redo),
                                tint =
                                    if (redoEnabled) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(
                                            alpha = 0.3f,
                                        )
                                    },
                            )
                        }
                        IconButton(onClick = onToggleSearch) {
                            Icon(
                                Icons.Default.Search,
                                stringResource(R.string.cd_search),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onScrollTop) {
                            Icon(
                                Icons.Default.VerticalAlignTop,
                                stringResource(R.string.top),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = onScrollBottom) {
                            Icon(
                                Icons.Default.VerticalAlignBottom,
                                stringResource(R.string.bottom),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                        }
                        IconButton(onClick = onToggleComment) {
                            Icon(
                                Icons.AutoMirrored.Filled.Comment,
                                stringResource(R.string.cd_comment),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                        IconButton(onClick = onToggleWrap) {
                            Icon(
                                if (isWrappingEnabled) Icons.AutoMirrored.Filled.WrapText else Icons.AutoMirrored.Filled.FormatAlignLeft,
                                contentDescription = stringResource(R.string.toggle_wrap),
                                tint = if (isWrappingEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    IconButton(onClick = onHide) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            stringResource(R.string.cd_hide_toolbar),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                LazyRow(
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        AccessoryKey(
                            "⏎",
                            width = 48.dp,
                            highlight = true,
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
    val initialText =
        """
def calculate_sum(a, b):
    result = a + b
    return result

# TODO: Add more functions
        """.trimIndent()

    var codeState by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length),
            ),
        )
    }
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            CodeEditor(
                code = codeState,
                onCodeChange = { codeState = it },
                interpreter = "python",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

fun getSnippetsForInterpreter(interpreter: String): List<Snippet> =
    when (interpreter.trim()) {
        "python", "python3" ->
            listOf(
                Snippet("def", "def name():\n    "),
                Snippet("if", "if condition:\n    "),
                Snippet("print", "print()"),
            )

        "node", "nodejs", "js" ->
            listOf(
                Snippet("func", "function name() {\n    \n}"),
                Snippet("log", "console.log()"),
                Snippet("if", "if (condition) {\n    \n}"),
            )

        "bash", "sh" ->
            listOf(
                Snippet("if", "if [ condition ]; then\n    \nfi"),
                Snippet("echo", "echo \"\""),
                Snippet("var", "VAR=\"value\""),
            )

        "cpp", "g++", "gcc" ->
            listOf(
                Snippet("main", "int main() {\n    return 0;\n}"),
                Snippet("inc", "#include <iostream>"),
                Snippet("out", "std::cout <<  << std::endl;"),
            )

        else -> emptyList()
    }

data class Snippet(
    val label: String,
    val code: String,
)
