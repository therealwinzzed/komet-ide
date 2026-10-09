package winzzed.komet.ide.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import winzzed.komet.ide.data.UpdateChecker
import winzzed.komet.ide.data.UpdateInfo
import winzzed.komet.ide.data.platformSupportsSelfUpdate
import winzzed.komet.ide.platform.exitApplication
import winzzed.komet.ide.ui.theme.KometIdeTheme

@Composable
fun App() {
    KometIdeTheme {
        var basePath by remember { mutableStateOf<String?>(null) }
        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var showUpdateDialog by remember { mutableStateOf(false) }
        var downloading by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var downloadedBytes by remember { mutableStateOf(0L) }
        var totalBytes by remember { mutableStateOf(0L) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val info = UpdateChecker.check()
            if (info != null) {
                updateInfo = info
                showUpdateDialog = true
            }
        }

        AnimatedContent(
            targetState = basePath,
            transitionSpec = {
                (fadeIn(tween(350)) + slideInHorizontally(
                    initialOffsetX = { it / 6 },
                    animationSpec = tween(350)
                )) togetherWith fadeOut(tween(250))
            },
            label = "root"
        ) { path ->
            if (path == null) {
                StartScreen(onProjectSelected = { basePath = it })
            } else {
                IdeScreen(
                    basePath = path,
                    onCloseProject = { basePath = null },
                    onCloseIde = { exitApplication() }
                )
            }
        }

        val info = updateInfo
        if (showUpdateDialog && info != null) {
            AlertDialog(
                onDismissRequest = { if (!downloading) showUpdateDialog = false },
                title = { Text("Доступно обновление ${info.version}") },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(info.description)
                        Spacer(Modifier.height(12.dp))
                        if (downloading) {
                            val progressPercent = (progress * 100).toInt()
                            val progressText = if (totalBytes > 0) {
                                "$progressPercent%  (${formatBytes(downloadedBytes)} / ${formatBytes(totalBytes)})"
                            } else {
                                formatBytes(downloadedBytes)
                            }
                            Text(text = "Загрузка... $progressText", fontSize = 12.sp)
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        errorMessage?.let { err ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ошибка: $err",
                                color = androidx.compose.ui.graphics.Color.Red,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                downloading = true
                                errorMessage = null
                                progress = 0f
                                downloadedBytes = 0L
                                totalBytes = 0L
                                try {
                                    val file = UpdateChecker.download(info.url) { d, t ->
                                        downloadedBytes = d
                                        totalBytes = t
                                        progress = if (t > 0) d.toFloat() / t else 0f
                                    }
                                    downloading = false
                                    if (file != null) {
                                        UpdateChecker.install(file)
                                        kotlinx.coroutines.delay(1500)
                                        exitApplication()
                                    } else {
                                        errorMessage = "Обновление недоступно на этой платформе"
                                    }
                                } catch (t: Throwable) {
                                    downloading = false
                                    errorMessage = t.message ?: "неизвестная ошибка"
                                    t.printStackTrace()
                                }
                            }
                        },
                        enabled = !downloading
                    ) {
                        Text(if (downloading) "Загрузка..." else "Обновить")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showUpdateDialog = false },
                        enabled = !downloading
                    ) { Text("Позже") }
                }
            )
        }
    }
}

private fun formatBytes(bytes: Long): String {
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