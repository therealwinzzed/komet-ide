package winzzed.komet.ide.platform

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import javax.swing.JFileChooser

actual val platformSupportsDirectoryPicker: Boolean = true

actual fun pickDirectory(
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

actual fun pickSaveFile(
    title: String,
    suggestedName: String,
    onResult: (String?) -> Unit
) {
    val chooser = JFileChooser().apply {
        dialogTitle = title
        selectedFile = File(suggestedName)
    }
    val result = chooser.showSaveDialog(null)
    onResult(
        if (result == JFileChooser.APPROVE_OPTION) chooser.selectedFile.absolutePath else null
    )
}

actual fun copyToClipboard(text: String) {
    try {
        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = StringSelection(text)
        clipboard.setContents(selection, selection)
    } catch (_: Throwable) {
    }
}

actual fun exitApplication() {
    kotlin.system.exitProcess(0)
}

actual fun temporaryDirectory(): String? = System.getProperty("java.io.tmpdir")
