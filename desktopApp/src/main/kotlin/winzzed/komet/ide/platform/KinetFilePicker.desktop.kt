package winzzed.komet.ide.platform

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * Desktop archive picker. Returns the file name and bytes so the caller does not
 * have to re-open the file.
 */
@Composable
fun rememberKinetFilePickerImpl(
    onResult: (name: String, bytes: ByteArray) -> Unit
): KinetFilePicker = KinetFilePicker {
    val chooser = JFileChooser().apply {
        dialogTitle = "Открыть плагин"
        fileSelectionMode = JFileChooser.FILES_ONLY
        isMultiSelectionEnabled = false
        fileFilter = FileNameExtensionFilter("Плагин Komet (*.kinet)", "kinet")
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@KinetFilePicker

    val file: File = chooser.selectedFile ?: return@KinetFilePicker
    try {
        onResult(file.name, file.readBytes())
    } catch (_: Throwable) {
        // Unreadable file: leave the UI unchanged.
    }
}
