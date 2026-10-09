package winzzed.komet.ide.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import winzzed.komet.ide.data.AndroidContext

/**
 * Android has no synchronous directory picker. The current UI has no Activity
 * launcher wired up, so picking is reported as unsupported rather than faked;
 * the caller treats a null result as "cancelled".
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
    val ctx = AndroidContext.appContext ?: return
    try {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Komet IDE", text))
    } catch (_: Throwable) {
    }
}

actual fun exitApplication() {
    android.os.Process.killProcess(android.os.Process.myPid())
}

actual fun temporaryDirectory(): String? =
    AndroidContext.appContext?.cacheDir?.absolutePath
