package winzzed.komet.ide.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import winzzed.komet.ide.data.AndroidContext

/**
 * Android has no synchronous directory picker. The current UI has no Activity
 * launcher wired up, so picking is reported as unsupported rather than faked;
 * the caller treats a null result as "cancelled".
 */
val platformSupportsNativeDialogs: Boolean = false

fun pickDirectory(
    title: String,
    initialPath: String?,
    onResult: (String?) -> Unit
) {
    onResult(null)
}

fun copyToClipboard(text: String) {
    val ctx = AndroidContext.appContext ?: return
    try {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Komet IDE", text))
    } catch (_: Throwable) {
    }
}

fun exitApplication() {
    android.os.Process.killProcess(android.os.Process.myPid())
}

/** Routes the system back gesture to [onBack] while [enabled]. */
@Composable
fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
