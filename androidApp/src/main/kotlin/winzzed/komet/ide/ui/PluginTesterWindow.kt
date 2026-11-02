package winzzed.komet.ide.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.data.PluginInfo
import winzzed.komet.ide.ui.theme.*

/** One line in the tester's output log. */
private data class TesterMessage(
    val text: String,
    val fromUser: Boolean
)

/**
 * The plugin tester window, opened by the Run button.
 *
 * Top: the plugin header (name, version · commands, description). Bottom: a chat
 * panel where the developer sends commands to the plugin and sees the output.
 */
@Composable
fun PluginTesterWindow(
    info: PluginInfo,
    onClose: () -> Unit
) {
    var messages by remember {
        mutableStateOf(
            listOf(
                TesterMessage(
                    "Плагин «${info.name}» загружен. Введите команду для проверки.",
                    fromUser = false
                )
            )
        )
    }
    var input by remember { mutableStateOf("") }

    val listState = rememberLazyListState()
    val focus = remember { FocusRequester() }

    val send: () -> Unit = {
        val text = input.trim()
        if (text.isNotEmpty()) {
            // Echo the request, then a stub reply. Real execution is not wired yet,
            // so the reply explains what would happen instead of faking output.
            messages = messages + TesterMessage(text, fromUser = true)
            messages = messages + TesterMessage(replyFor(text, info), fromUser = false)
            input = ""
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            ),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(modifier = Modifier.padding(22.dp)) {
            val compact = maxWidth < 620.dp
            val dialogWidth = if (compact) maxWidth else 720.dp
            val dialogHeight = if (compact) (maxHeight - 24.dp).coerceAtMost(640.dp) else 600.dp

            Column(
                modifier = Modifier
                    .width(dialogWidth)
                    .height(dialogHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(IdePopup)
                    .border(1.dp, IdeBorderStrong, RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // ---- Header: title strip -------------------------------------
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .background(IdeWindow)
                        .padding(start = 14.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Тестирование плагина",
                        color = IdeText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    ToolbarIconButton(
                        icon = Icons.Filled.Close,
                        contentDescription = "Закрыть",
                        onClick = onClose
                    )
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

                // ---- Plugin info block ---------------------------------------
                PluginInfoBlock(info)
                Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

                // ---- Output log ----------------------------------------------
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(IdeEditor)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages) { message ->
                        TesterMessageRow(message)
                    }
                }

                Box(Modifier.fillMaxWidth().height(1.dp).background(IdeBorder))

                // ---- Input row -----------------------------------------------
                TesterInputRow(
                    value = input,
                    onValueChange = { input = it },
                    onSend = send,
                    focusRequester = focus
                )
            }
        }
    }
}

@Composable
private fun PluginInfoBlock(info: PluginInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(IdeToolWindow)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Whatshot,
                contentDescription = null,
                tint = IdeBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                info.name,
                color = IdeText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                info.version,
                color = IdeBlue,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(" · ", color = IdeTextMuted, fontSize = 11.sp)
            Text(
                if (info.commands.isEmpty()) {
                    "нет команд"
                } else {
                    info.commands.joinToString(", ")
                },
                color = IdeTextDim,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (info.description.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                info.description,
                color = IdeTextDim,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun TesterMessageRow(message: TesterMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (message.fromUser) IdeBlue else IdeToolWindow)
                .border(
                    1.dp,
                    if (message.fromUser) IdeBlue else IdeBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 11.dp, vertical = 8.dp)
        ) {
            Text(
                message.text,
                color = if (message.fromUser) Color.White else IdeText,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun TesterInputRow(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    focusRequester: FocusRequester
) {
    val canSend = value.isNotBlank()
    val sendTint by animateColorAsState(
        targetValue = if (canSend) IdeBlue else IdeHover,
        animationSpec = tween(150),
        label = "sendTint"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(IdeToolWindow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(IdeWindow)
                .border(1.dp, IdeBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text("Введите команду…", color = IdeTextMuted, fontSize = 12.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = IdeText, fontSize = 12.sp),
                cursorBrush = SolidColor(IdeText),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                // Enter sends; Shift+Enter stays available for future multi-line input.
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            onSend()
                            true
                        } else false
                    }
            )
        }

        Spacer(Modifier.width(8.dp))

        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()

        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (hovered && canSend) IdeBlueHover else sendTint)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    enabled = canSend,
                    onClick = onSend
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Отправить",
                tint = if (canSend) Color.White else IdeTextMuted,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

/**
 * What the tester prints back for a command. Execution of real plugin code is not
 * wired up yet, so this is an explicit placeholder rather than invented output.
 */
private fun replyFor(command: String, info: PluginInfo): String {
    val known = info.commands.firstOrNull { it.equals(command, ignoreCase = true) }
    return if (known != null) {
        "Команда «$known» найдена в манифесте. Выполнение плагина пока не подключено."
    } else {
        "Команда «$command» не объявлена в манифесте плагина."
    }
}
