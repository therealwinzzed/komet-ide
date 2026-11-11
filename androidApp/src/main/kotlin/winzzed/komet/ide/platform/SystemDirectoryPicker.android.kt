package winzzed.komet.ide.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * Android implementation backed by the system Storage Access Framework picker.
 *
 * The result is a display label plus the tree URI; callers must keep the URI for
 * any subsequent I/O, the label is only shown in the UI.
 */
@Composable
fun rememberSystemDirectoryPickerImpl(
    onResult: (path: String, uri: String?) -> Unit
): SystemDirectoryPicker {
    val pickers = rememberSafPickers()
    val currentOnResult by rememberUpdatedState(onResult)

    return remember(pickers) {
        SystemDirectoryPicker(isSupported = true) {
            pickers.launchDirectory { picked ->
                if (picked != null) {
                    currentOnResult(picked.path, picked.uri?.toString())
                }
            }
        }
    }
}
