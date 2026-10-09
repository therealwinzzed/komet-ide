package winzzed.komet.ide.platform

import platform.UIKit.UIPasteboard

/**
 * iOS has no synchronous directory picker without a UIViewController bridge, so
 * picking is reported as unsupported and callers cancel gracefully.
 */
actual val platformSupportsDirectoryPicker: Boolean = false

actual fun pickDirectory(
    title: String,
    initialPath: String?,
    onResult: (String?) -> Unit
) {
    onResult(null)
}

actual fun pickSaveFile(
    title: String,
    suggestedName: String,
    onResult: (String?) -> Unit
) {
    onResult(null)
}

actual fun copyToClipboard(text: String) {
    try {
        UIPasteboard.generalPasteboard.string = text
    } catch (_: Throwable) {
    }
}

/** iOS apps must not terminate themselves programmatically. */
actual fun exitApplication() {
}

actual fun temporaryDirectory(): String? =
    platform.Foundation.NSTemporaryDirectory()
