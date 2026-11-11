package winzzed.komet.ide.platform

import androidx.compose.runtime.Composable

/**
 * Desktop already has a working native folder chooser ([pickDirectory]), so the
 * caller does not need this asynchronous variant; report it as unsupported.
 */
@Composable
fun rememberSystemDirectoryPickerImpl(
    onResult: (path: String, uri: String?) -> Unit
): SystemDirectoryPicker = SystemDirectoryPicker(isSupported = false, open = { })
