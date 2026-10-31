package winzzed.komet.ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.platform.copyToClipboard
import winzzed.komet.ide.ui.theme.*

@Composable
fun Editor(
    fileName: String,
    text: String,
    onChange: (String) -> Unit,
    showHeader: Boolean = true
) {
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    val selectionColors = TextSelectionColors(
        handleColor = TextPrimary,
        backgroundColor = Color(0x55808080)
    )

    val language = when {
        fileName.endsWith(".json") -> "json"
        fileName.endsWith(".js") -> "javascript"
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IdeEditor)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        if (showHeader) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(tween(250)) + slideInVertically(
                    initialOffsetY = { -it / 2 },
                    animationSpec = tween(250)
                )
            ) {
                Text(fileName, color = TextSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(6.dp))
        }

        if (fileName == "Help.md") {
            var menuExpanded by remember { mutableStateOf(false) }
            var menuOffsetX by remember { mutableStateOf(0f) }
            var menuOffsetY by remember { mutableStateOf(0f) }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(vScroll)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.type == PointerEventType.Press &&
                                        event.buttons.isSecondaryPressed
                                    ) {
                                        val pos = event.changes.first().position
                                        menuOffsetX = pos.x
                                        menuOffsetY = pos.y
                                        menuExpanded = true
                                    }
                                }
                            }
                        }
                ) {
                    SelectionContainer {
                        MarkdownText(
                            content = text,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        offset = DpOffset(menuOffsetX.dp, menuOffsetY.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Копировать всё", color = TextPrimary) },
                            onClick = {
                                copyToClipboard(text)
                                menuExpanded = false
                            }
                        )
                    }
                }
            }
        } else if (language != null) {
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(IdeEditor)
                        .verticalScroll(vScroll)
                        .horizontalScroll(hScroll)
                        .padding(4.dp)
                ) {
                    var fieldValue by remember(fileName) {
                        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
                    }

                    LaunchedEffect(text) {
                        if (fieldValue.text != text) {
                            fieldValue = TextFieldValue(text, selection = TextRange(text.length))
                        }
                    }

                    AutoCompleteField(
                        value = fieldValue,
                        onValueChange = { newValue ->
                            fieldValue = newValue
                            onChange(newValue.text)
                        },
                        fileName = fileName,
                        visualTransformation = when {
                            fileName.endsWith(".json") -> JsonSyntaxHighlight()
                            fileName.endsWith(".js") -> JavaScriptSyntaxHighlight()
                            else -> VisualTransformation.None
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(IdeEditor)
                        .verticalScroll(vScroll)
                        .horizontalScroll(hScroll)
                        .padding(4.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = onChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            color = TextPrimary,
                            fontSize = 13.sp
                        ),
                        cursorBrush = SolidColor(IdeText),
                        maxLines = Int.MAX_VALUE
                    )
                }
            }
        }
    }
}