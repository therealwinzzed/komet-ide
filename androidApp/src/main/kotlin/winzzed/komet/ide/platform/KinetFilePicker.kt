package winzzed.komet.ide.platform

import androidx.compose.runtime.Composable

/**
 * Platform hook for picking a `.kinet` archive.
 *
 * [rememberKinetFilePickerImpl] is provided per platform: desktop uses JFileChooser,
 * Android the system document picker.
 */
@Composable
fun rememberKinetFilePicker(
    onResult: (name: String, bytes: ByteArray) -> Unit
): KinetFilePicker = rememberKinetFilePickerImpl(onResult)

/** Imperative handle returned by [rememberKinetFilePicker]. */
class KinetFilePicker internal constructor(private val open: () -> Unit) {
    fun show() = open()
}
