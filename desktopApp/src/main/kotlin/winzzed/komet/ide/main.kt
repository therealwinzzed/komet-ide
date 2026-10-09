package winzzed.komet.ide

import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import winzzed.komet.ide.ui.App
import winzzed.komet.ide.ui.EditorState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Komet IDE",
        onPreviewKeyEvent = { event ->
            if (event.type == KeyEventType.KeyDown) {
                EditorState.handleKey(event)
            } else false
        }
    ) {
        App()
    }
}
