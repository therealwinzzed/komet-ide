package winzzed.komet.ide.platform

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import winzzed.komet.ide.data.AndroidContext

/**
 * Android does have a folder picker, but it is asynchronous (the result arrives
 * after the activity resumes) and returns a SAF tree URI instead of an absolute
 * path. Callers that can only accept a path synchronously therefore see this as
 * unsupported; the IDE uses [SafPickers] for the real picker.
 */
val platformSupportsNativeDialogs: Boolean = false

fun pickDirectory(
    title: String,
    initialPath: String?,
    onResult: (String?) -> Unit
) {
    // The composable call sites go through SafPickers.pickDirectory() instead.
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
