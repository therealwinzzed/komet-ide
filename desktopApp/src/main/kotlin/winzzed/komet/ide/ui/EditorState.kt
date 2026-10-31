package winzzed.komet.ide.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

object EditorState {
    var currentValue: TextFieldValue by mutableStateOf(TextFieldValue(""))
        internal set

    var onChange: ((TextFieldValue) -> Unit)? = null
        private set

    var isFocused: Boolean by mutableStateOf(false)
        private set

    fun register(value: TextFieldValue, onChange: (TextFieldValue) -> Unit) {
        this.currentValue = value
        this.onChange = onChange
    }

    fun unregister() {
        this.onChange = null
        this.isFocused = false
    }

    fun updateFocus(focused: Boolean) {
        this.isFocused = focused
    }

    fun handleKey(event: KeyEvent): Boolean {
        val callback = onChange ?: return false
        if (!isFocused) return false

        val value = currentValue
        val text = value.text
        val cursor = value.selection.start

        return when (event.key) {
            Key.DirectionLeft -> {
                val newCursor = (cursor - 1).coerceAtLeast(0)
                val newValue = value.copy(selection = TextRange(newCursor))
                currentValue = newValue
                callback(newValue)
                true
            }
            Key.DirectionRight -> {
                val newCursor = (cursor + 1).coerceAtMost(text.length)
                val newValue = value.copy(selection = TextRange(newCursor))
                currentValue = newValue
                callback(newValue)
                true
            }
            Key.DirectionUp -> {
                val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
                    .let { if (it == -1) 0 else it + 1 }
                val prevLineStart = if (lineStart == 0) 0
                else text.lastIndexOf('\n', (lineStart - 2).coerceAtLeast(0))
                    .let { if (it == -1) 0 else it + 1 }
                val column = cursor - lineStart
                val newCursor = (prevLineStart + column).coerceAtMost(lineStart - 1)
                val newValue = value.copy(selection = TextRange(newCursor.coerceAtLeast(0)))
                currentValue = newValue
                callback(newValue)
                true
            }
            Key.DirectionDown -> {
                val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
                    .let { if (it == -1) 0 else it + 1 }
                val lineEnd = text.indexOf('\n', cursor)
                    .let { if (it == -1) text.length else it }
                val nextLineStart = if (lineEnd >= text.length) text.length else lineEnd + 1
                val nextLineEnd = if (nextLineStart >= text.length) text.length
                else text.indexOf('\n', nextLineStart)
                    .let { if (it == -1) text.length else it }
                val column = cursor - lineStart
                val newCursor = (nextLineStart + column).coerceAtMost(nextLineEnd)
                val newValue = value.copy(selection = TextRange(newCursor))
                currentValue = newValue
                callback(newValue)
                true
            }
            else -> false
        }
    }
}