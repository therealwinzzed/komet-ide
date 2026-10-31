package winzzed.komet.ide.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Minimize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import winzzed.komet.ide.data.CURRENT_VERSION
import winzzed.komet.ide.data.UpdateInfo
import winzzed.komet.ide.ui.theme.*

/**
 * The update flow, modelled on the bank app: Material3 dialogs with a circular
 * badge, one primary action and an optional "later" action.
 *
 * While downloading, the dialog can be collapsed into [FloatingDownloadButton],
 * which stays above every screen so progress remains reachable.
 */
enum class UpdateStage { AVAILABLE, DOWNLOADING, READY, LATEST }

@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDownload: () -> Unit,
    onLater: () -> Unit
) {
    UpdateDialogShell(onDismissRequest = onLater) {
        UpdateBadge(Icons.Filled.KeyboardArrowDown)

        Spacer(Modifier.height(14.dp))
        Text(
            "Обновление ${info.version}",
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = IdeText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            info.description.ifBlank { "Доступна новая версия приложения" },
            fontSize = 13.sp,
            color = IdeTextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IdeTextDim),
                border = BorderStroke(1.dp, IdeBorderStrong)
            ) {
                Text("Позже", fontSize = 13.sp)
            }
            Button(
                onClick = onDownload,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IdeBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Скачать", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Progress dialog. "Свернуть" hides it and hands control to the floating
 * button; the download itself keeps running in the caller's coroutine.
 */
@Composable
fun UpdateDownloadDialog(
    info: UpdateInfo,
    progress: Float,
    downloadedBytes: Long,
    totalBytes: Long,
    errorMessage: String?,
    onCollapse: () -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(220),
        label = "updateProgress"
    )
    val percent = (animated * 100).toInt()

    UpdateDialogShell(onDismissRequest = onCollapse) {
        UpdateBadge(Icons.Filled.KeyboardArrowDown)

        Spacer(Modifier.height(14.dp))
        Text(
            "Обновление ${info.version}",
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = IdeText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = IdeBlue,
            trackColor = IdeBorder
        )

        Spacer(Modifier.height(12.dp))
        Text(
            "$percent%",
            fontSize = 13.sp,
            color = IdeTextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (totalBytes > 0) {
            Spacer(Modifier.height(2.dp))
            Text(
                "${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)}",
                fontSize = 11.sp,
                color = IdeTextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        errorMessage?.let { err ->
            Spacer(Modifier.height(10.dp))
            Text(
                "Ошибка: $err",
                fontSize = 11.sp,
                color = IdeRed,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onCollapse,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = IdeText),
            border = BorderStroke(1.dp, IdeBorderStrong)
        ) {
            Icon(Icons.Filled.Minimize, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Свернуть", fontSize = 13.sp)
        }
    }
}

@Composable
fun UpdateReadyDialog(info: UpdateInfo, onInstall: () -> Unit, onLater: () -> Unit) {
    UpdateDialogShell(onDismissRequest = onLater) {
        UpdateBadge(Icons.Filled.Check)

        Spacer(Modifier.height(14.dp))
        Text(
            "Обновление ${info.version} загружено",
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = IdeText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Установка закроет Komet IDE, после чего приложение перезапустится",
            fontSize = 13.sp,
            color = IdeTextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IdeTextDim),
                border = BorderStroke(1.dp, IdeBorderStrong)
            ) {
                Text("Позже", fontSize = 13.sp)
            }
            Button(
                onClick = onInstall,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IdeBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Установить", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun UpdateLatestDialog(onClose: () -> Unit) {
    UpdateDialogShell(onDismissRequest = onClose) {
        UpdateBadge(Icons.Filled.Check)

        Spacer(Modifier.height(14.dp))
        Text(
            "Обновлений нет",
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = IdeText,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Установлена последняя версия Komet IDE $CURRENT_VERSION",
            fontSize = 13.sp,
            color = IdeTextDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = IdeBlue,
                contentColor = Color.White
            )
        ) {
            Text("Закрыть", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * The collapsed download indicator: a circular progress ring pinned to the
 * corner of the window. Clicking it restores the full progress dialog.
 */
@Composable
fun FloatingDownloadButton(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "floatingProgress"
    )
    val percent = (animated * 100).toInt()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (hovered) IdePopup else IdeToolWindow)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animated },
            modifier = Modifier.size(52.dp),
            strokeWidth = 3.dp,
            color = IdeBlue,
            trackColor = IdeBorder
        )
        Text(
            "$percent%",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = IdeText
        )
    }
}

@Composable
private fun UpdateDialogShell(
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = IdePopup,
            border = BorderStroke(1.dp, IdeBorderStrong),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content
            )
        }
    }
}

@Composable
private fun UpdateBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(IdeBlue.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = IdeBlue,
            modifier = Modifier.size(26.dp)
        )
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "${roundTo1(kb)} KB"
    val mb = kb / 1024.0
    return "${roundTo2(mb)} MB"
}

/** Rounds to one decimal place without java.lang.String.format. */
private fun roundTo1(value: Double): String {
    val scaled = kotlin.math.round(value * 10.0).toLong()
    return "${scaled / 10}.${scaled % 10}"
}

/** Rounds to two decimal places without java.lang.String.format. */
private fun roundTo2(value: Double): String {
    val scaled = kotlin.math.round(value * 100.0).toLong()
    val frac = (scaled % 100).toInt()
    val fracStr = if (frac < 10) "0$frac" else "$frac"
    return "${scaled / 100}.$fracStr"
}
