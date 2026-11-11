package winzzed.komet.ide.platform

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import winzzed.komet.ide.data.ScopedStorage

/**
 * Android archive picker backed by the system document picker. The file is read
 * immediately because the temporary read grant only lasts while the activity
 * result is handled.
 */
@Composable
fun rememberKinetFilePickerImpl(
    onResult: (name: String, bytes: ByteArray) -> Unit
): KinetFilePicker {
    val pickers = rememberSafPickers()
    val currentOnResult by rememberUpdatedState(onResult)

    return remember(pickers) {
        KinetFilePicker {
            pickers.launchKinetFile { uri: Uri? ->
                if (uri == null) return@launchKinetFile
                val bytes = ScopedStorage.readBytes(uri) ?: return@launchKinetFile
                currentOnResult(ScopedStorage.displayNameFor(uri), bytes)
            }
        }
    }
}
