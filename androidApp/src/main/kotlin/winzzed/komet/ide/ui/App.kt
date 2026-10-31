package winzzed.komet.ide.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import winzzed.komet.ide.data.UpdateChecker
import winzzed.komet.ide.data.UpdateInfo
import winzzed.komet.ide.data.platformSupportsSelfUpdate
import winzzed.komet.ide.platform.exitApplication
import winzzed.komet.ide.ui.theme.KometIdeTheme

/**
 * Root composable.
 *
 * The update check runs once at startup. While it downloads, the dialog can be
 * collapsed into a floating button that survives navigating between the welcome
 * screen and the editor.
 */
@Composable
fun App() {
    KometIdeTheme {
        var basePath by remember { mutableStateOf<String?>(null) }

        var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
        var stage by remember { mutableStateOf(UpdateStage.AVAILABLE) }
        var dialogVisible by remember { mutableStateOf(false) }
        var progress by remember { mutableStateOf(0f) }
        var downloadedBytes by remember { mutableStateOf(0L) }
        var totalBytes by remember { mutableStateOf(0L) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var downloadedPath by remember { mutableStateOf<String?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val info = UpdateChecker.check()
            if (info != null) {
                updateInfo = info
                stage = UpdateStage.AVAILABLE
                dialogVisible = true
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val path = basePath
            if (path == null) {
                StartScreen(onProjectSelected = { basePath = it })
            } else {
                IdeScreen(
                    basePath = path,
                    onCloseProject = { basePath = null },
                    onCloseIde = { exitApplication() }
                )
            }

            val info = updateInfo
            val floating = info != null &&
                stage == UpdateStage.DOWNLOADING &&
                !dialogVisible

            AnimatedVisibility(
                visible = floating,
                enter = fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f),
                exit = fadeOut(tween(150)) + scaleOut(tween(150), targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 44.dp)
            ) {
                FloatingDownloadButton(
                    progress = progress,
                    onClick = { dialogVisible = true }
                )
            }
        }

        val info = updateInfo
        if (info != null && dialogVisible) {
            when (stage) {
                UpdateStage.AVAILABLE -> UpdateAvailableDialog(
                    info = info,
                    onDownload = {
                        stage = UpdateStage.DOWNLOADING
                        progress = 0f
                        downloadedBytes = 0L
                        totalBytes = 0L
                        errorMessage = null
                        scope.launch {
                            try {
                                val file = UpdateChecker.download(info.url) { d, t ->
                                    downloadedBytes = d
                                    totalBytes = t
                                    progress = if (t > 0) d.toFloat() / t else 0f
                                }
                                if (file != null) {
                                    downloadedPath = file
                                    progress = 1f
                                    stage = UpdateStage.READY
                                } else {
                                    errorMessage = if (platformSupportsSelfUpdate) {
                                        "Не удалось скачать обновление"
                                    } else {
                                        "Обновление недоступно на этой платформе"
                                    }
                                }
                            } catch (t: Throwable) {
                                errorMessage = t.message ?: "неизвестная ошибка"
                            }
                        }
                    },
                    onLater = { dialogVisible = false }
                )

                UpdateStage.DOWNLOADING -> UpdateDownloadDialog(
                    info = info,
                    progress = progress,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes,
                    errorMessage = errorMessage,
                    onCollapse = { dialogVisible = false }
                )

                UpdateStage.READY -> UpdateReadyDialog(
                    info = info,
                    onInstall = {
                        scope.launch {
                            val target = downloadedPath
                            if (target != null) UpdateChecker.install(target)
                            delay(1200)
                            exitApplication()
                        }
                    },
                    onLater = { dialogVisible = false }
                )

                UpdateStage.LATEST -> UpdateLatestDialog(onClose = { dialogVisible = false })
            }
        }
    }
}
