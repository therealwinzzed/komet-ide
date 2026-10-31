package winzzed.komet.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import winzzed.komet.ide.ui.theme.*

private val JSON_SUGGESTIONS = listOf(
    "schemaVersion", "id", "name", "version", "apiVersion",
    "entry", "description", "author", "permissions", "commands",
    "main", "updateUrl"
)

private val JS_SUGGESTIONS = listOf(
    "function ", "return ", "const ", "let ", "var ",
    "if (", "else ", "for (", "while (", "console.log(",
    "registerCommand", "getPlayer", "sendMessage", "import", "async",
    "export"
)

private class BoundedPopupPositionProvider(
    private val anchorOffset: IntOffset
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        var x = anchorBounds.left + anchorOffset.x
        var y = anchorBounds.top + anchorOffset.y

        if (x + popupContentSize.width > windowSize.width) {
            x = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        }
        if (y + popupContentSize.height > windowSize.height) {
            y = (anchorBounds.top + anchorOffset.y - popupContentSize.height)
                .coerceAtLeast(0)
        }
        if (x < 0) x = 0
        if (y < 0) y = 0
        return IntOffset(x, y)
    }
}

@Composable
fun AutoCompleteField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    fileName: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    DisposableEffect(Unit) {
        EditorState.register(value, onValueChange)
        EditorState.updateFocus(true)
        onDispose {
            EditorState.unregister()
            EditorState.updateFocus(false)
        }
    }

    LaunchedEffect(value) {
        EditorState.currentValue = value
    }

    val suggestions = when {
        fileName.endsWith(".json") -> JSON_SUGGESTIONS
        fileName.endsWith(".js") -> JS_SUGGESTIONS
        else -> emptyList()
    }

    val currentWord = remember(value.text, value.selection) {
        val words = value.text.take(value.selection.start)
            .split(Regex("[^a-zA-Z0-9_$]"))
        words.lastOrNull() ?: ""
    }

    val filtered = remember(currentWord, suggestions) {
        if (currentWord.isBlank()) emptyList()
        else suggestions.filter { it.startsWith(currentWord, ignoreCase = true) }.take(8)
    }

    LaunchedEffect(currentWord, filtered) {
        expanded = currentWord.isNotBlank() &&
                currentWord.first().isLetter() &&
                filtered.isNotEmpty()
    }

    val density = LocalDensity.current
    val cursorOffset = remember(value.selection, textLayoutResult) {
        val layout = textLayoutResult ?: return@remember IntOffset.Zero
        val safeOffset = value.selection.start
            .coerceIn(0, layout.layoutInput.text.length)
        val cursorRect = layout.getCursorRect(safeOffset)
        with(density) {
            IntOffset(
                x = cursorRect.left.toInt(),
                y = cursorRect.bottom.toInt()
            )
        }
    }

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
                fontSize = 13.sp
            ),
            cursorBrush = SolidColor(TextPrimary),
            maxLines = Int.MAX_VALUE,
            visualTransformation = visualTransformation,
            onTextLayout = { textLayoutResult = it }
        )

        if (expanded && filtered.isNotEmpty()) {
            Popup(
                popupPositionProvider = BoundedPopupPositionProvider(cursorOffset),
                properties = PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                ),
                onDismissRequest = { expanded = false }
            ) {
                LazyColumn(
                    modifier = Modifier
                        .width(220.dp)
                        .shadow(4.dp)
                        .background(SurfaceHigh)
                        .padding(4.dp)
                ) {
                    items(filtered) { suggestion ->
                        Text(
                            text = suggestion,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val before = value.text.take(
                                        value.selection.start - currentWord.length
                                    )
                                    val after = value.text.drop(value.selection.start)
                                    val newText = before + suggestion + after
                                    onValueChange(
                                        TextFieldValue(
                                            text = newText,
                                            selection = TextRange(before.length + suggestion.length)
                                        )
                                    )
                                    expanded = false
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}