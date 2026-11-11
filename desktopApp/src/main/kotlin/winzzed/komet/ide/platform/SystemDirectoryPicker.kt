package winzzed.komet.ide.platform

import androidx.compose.runtime.Composable

/**
 * Platform hook for opening a folder picker.
 *
 * Desktop returns an unsupported handle so the caller falls back to the in-app
 * browser; Android returns a handle that launches the system SAF picker.
 */
@Composable
fun rememberSystemDirectoryPicker(
    onResult: (path: String, uri: String?) -> Unit
): SystemDirectoryPicker = rememberSystemDirectoryPickerImpl(onResult)

/** Imperative handle returned by [rememberSystemDirectoryPicker]. */
class SystemDirectoryPicker internal constructor(
    val isSupported: Boolean,
    private val open: (initialPath: String?) -> Unit
) {
    /** Opens the picker; [initialPath] is only a hint and may be ignored. */
    fun show(initialPath: String?) = open(initialPath)
}
