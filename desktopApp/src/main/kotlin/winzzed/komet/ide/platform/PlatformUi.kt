package winzzed.komet.ide.platform

import androidx.compose.runtime.Composable
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser

val platformSupportsNativeDialogs: Boolean = true

fun pickDirectory(
    title: String,
    initialPath: String?,
    onResult: (String?) -> Unit
) {
    val chooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        dialogTitle = title
        if (initialPath != null) {
            val dir = File(initialPath)
            if (dir.isDirectory) currentDirectory = dir
        }
    }
    val result = chooser.showOpenDialog(null)
    onResult(
        if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
    )
}

fun copyToClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    } catch (_: Throwable) {
    }
}

fun exitApplication() {
    kotlin.system.exitProcess(0)
}

/** Desktop windows have no system back gesture; intentionally a no-op. */
@Composable
fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
