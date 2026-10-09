package winzzed.komet.ide.platform

private external fun clipboardWriteText(text: String)

/**
 * Browsers cannot hand back an absolute directory path from a picker, so the
 * picker is reported as unsupported and the caller cancels gracefully.
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
        clipboardWriteText(text)
    } catch (_: Throwable) {
    }
}

/** A browser tab cannot close itself reliably; no-op. */
actual fun exitApplication() {
}

/** No filesystem, so no temp directory. */
actual fun temporaryDirectory(): String? = null
