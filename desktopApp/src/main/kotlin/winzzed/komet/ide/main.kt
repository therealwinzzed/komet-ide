package winzzed.komet.ide

import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import winzzed.komet.ide.ui.App
import winzzed.komet.ide.ui.EditorState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Komet IDE",
        state = rememberWindowState(size = DpSize(1280.dp, 800.dp)),
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                EditorState.handleKey(event)
            } else false
        }
    ) {
        App()
    }
}
