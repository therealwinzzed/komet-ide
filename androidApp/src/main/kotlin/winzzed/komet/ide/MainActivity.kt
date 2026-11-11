package winzzed.komet.ide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import winzzed.komet.ide.data.AndroidContext
import winzzed.komet.ide.data.ensureProjectsDirectory
import winzzed.komet.ide.ui.App
import winzzed.komet.ide.ui.theme.IdeWindow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Required before any storage access (SharedPreferences / files dir).
        AndroidContext.init(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            // Makes /storage/emulated/0/KometIDEProjects exist before the UI asks
            // for the default projects directory, falling back to app storage when
            // shared storage is not writable.
            remember { ensureProjectsDirectory() }

            // The IDE draws its own chrome, so the window background must match to
            // avoid a white flash behind the system bars.
            Surface(modifier = Modifier.fillMaxSize(), color = IdeWindow) {
                AppWithInsets {
                    App()
                }
            }
        }
    }
}

/**
 * Edge-to-edge means the IDE would otherwise draw underneath the status and
 * navigation bars. Desktop has no system bars, so this padding lives here rather
 * than inside [App], which keeps the shared UI identical across platforms.
 */
@Composable
private fun AppWithInsets(content: @Composable () -> Unit) {
    val insets = WindowInsets.statusBars
        .union(WindowInsets.displayCutout)
        .union(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Vertical)

    Surface(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(insets),
        color = IdeWindow
    ) {
        content()
    }
}

@Preview(showSystemUi = true)
@Composable
fun AppAndroidPreview() {
    App()
}
