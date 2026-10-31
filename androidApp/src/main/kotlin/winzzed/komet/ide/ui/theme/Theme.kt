package winzzed.komet.ide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * A Material3 scheme mapped onto the IntelliJ palette. The update dialogs are
 * built from Material3 components, so they need a scheme that matches the IDE
 * chrome around them.
 */
private val IdeColors = darkColorScheme(
    primary = IdeBlue,
    onPrimary = IdeText,
    primaryContainer = IdeBluePressed,
    onPrimaryContainer = IdeText,
    secondary = IdeTextDim,
    onSecondary = IdeWindow,
    background = IdeWindow,
    onBackground = IdeText,
    surface = IdePanel,
    onSurface = IdeText,
    surfaceVariant = IdeHover,
    onSurfaceVariant = IdeTextDim,
    surfaceContainer = IdePanel,
    surfaceContainerHigh = IdeToolWindow,
    surfaceContainerHighest = IdeBorderStrong,
    surfaceContainerLow = IdeEditor,
    outline = IdeBorder,
    outlineVariant = IdeBorderStrong,
    error = IdeRed,
    onError = IdeText
)

@Composable
fun KometIdeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = IdeColors,
        typography = AppTypography,
        content = content
    )
}
