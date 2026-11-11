package winzzed.komet.ide.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import winzzed.komet.ide.data.defaultProjectsDirectory
import winzzed.komet.ide.data.fileNameOf
import winzzed.komet.ide.data.listDirectories
import winzzed.komet.ide.data.parentPathOf
import winzzed.komet.ide.data.pathExists
import winzzed.komet.ide.platform.pickDirectory
import winzzed.komet.ide.platform.platformSupportsNativeDialogs
import winzzed.komet.ide.platform.rememberSystemDirectoryPicker
import winzzed.komet.ide.ui.theme.*

/**
 * In-app directory browser used where the platform has no native folder picker
 * (Android, iOS, web). Android in particular cannot hand a writable absolute path
 * back from the system picker without SAF permissions, so projects live in the
 * app's own storage and are browsed here.
 *
 * [rootPath] bounds navigation so the user cannot wander outside the project area.
 */
@Composable
fun DirectoryChooserDialog(
    title: String,
    rootPath: String,
    startPath: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val initial = remember(rootPath, startPath) {
        val candidate = startPath?.takeIf { it.isNotBlank() && pathExists(it) } ?: rootPath
        candidate
    }
    var current by remember(initial) { mutableStateOf(initial) }

    val directories = remember(current) {
        runCatching { listDirectories(current) }.getOrDefault(emptyList())
    }
    val canGoUp = parentPathOf(current) != null && current != rootPath

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Breadcrumb of the current location.
                Text(
                    text = current,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 2
                )
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { parentPathOf(current)?.let { current = it } },
                        enabled = canGoUp
                    ) { Text("Вверх") }
                    OutlinedButton(
                        onClick = { current = rootPath },
                        enabled = current != rootPath
                    ) { Text("В корень") }
                }

                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = OutlineGray)
                Spacer(Modifier.height(6.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (directories.isEmpty()) {
                        Text(
                            "Вложенных папок нет",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        directories.forEach { name ->
                            val child = "$current/$name"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { current = child }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    name,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSelect(current) }) {
                Text("Выбрать эту папку")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

/**
 * Opens the platform picker when one exists, otherwise the in-app browser rooted at
 * [fallbackRoot]. [onResult] receives the chosen path or null.
 *
 * [onResultWithUri] additionally receives the SAF tree URI on Android; the path is
 * then only a display label and all I/O must go through the URI.
 */
@Composable
fun rememberDirectoryChooser(
    fallbackRoot: String,
    onResult: (String?) -> Unit
): DirectoryChooserController =
    rememberDirectoryChooser(fallbackRoot, onResultWithUri = { path, _ -> onResult(path) })

@Composable
fun rememberDirectoryChooser(
    fallbackRoot: String,
    onResultWithUri: (path: String, uri: String?) -> Unit
): DirectoryChooserController {
    var request by remember { mutableStateOf<DirectoryRequest?>(null) }

    // The controller is remembered across recompositions, so read the latest
    // callback instead of capturing the one from the first composition.
    val currentOnResult by rememberUpdatedState(onResultWithUri)
    val systemPicker = rememberSystemDirectoryPicker { path, uri ->
        currentOnResult(path, uri)
    }

    val controller = remember(systemPicker) {
        DirectoryChooserController { title, startPath ->
            when {
                // Desktop: a blocking native chooser returning an absolute path.
                platformSupportsNativeDialogs -> pickDirectory(title, startPath) { path ->
                    if (path != null) currentOnResult(path, null)
                }

                // Android: the asynchronous SAF picker, offering Documents/Downloads/etc.
                systemPicker.isSupported -> systemPicker.show(startPath)

                // Fallback: the in-app browser, used when no system picker exists.
                else -> request = DirectoryRequest(title, startPath)
            }
        }
    }

    request?.let { req ->
        DirectoryChooserDialog(
            title = req.title,
            rootPath = fallbackRoot,
            startPath = req.startPath,
            onDismiss = { request = null },
            onSelect = { path ->
                request = null
                currentOnResult(path, null)
            }
        )
    }

    return controller
}

private data class DirectoryRequest(val title: String, val startPath: String?)

/** Imperative handle so non-composable callbacks can request a directory. */
class DirectoryChooserController internal constructor(
    private val open: (title: String, startPath: String?) -> Unit
) {
    fun show(title: String, startPath: String? = null) = open(title, startPath)
}

/** Root used by the in-app browser: the app's own projects directory. */
fun projectsRootDirectory(): String = defaultProjectsDirectory()