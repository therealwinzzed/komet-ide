package winzzed.komet.ide.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import winzzed.komet.ide.ui.theme.*

@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier
) {
    Markdown(
        content = content,
        colors = markdownColor(
            text = TextPrimary,
            codeBackground = SurfaceHigh,
            inlineCodeBackground = SurfaceHigh,
            dividerColor = OutlineGray
        ),
        typography = markdownTypography(
            text = androidx.compose.ui.text.TextStyle(color = TextPrimary),
            h1 = androidx.compose.ui.text.TextStyle(color = TextPrimary),
            h2 = androidx.compose.ui.text.TextStyle(color = TextPrimary),
            h3 = androidx.compose.ui.text.TextStyle(color = TextPrimary),
            code = androidx.compose.ui.text.TextStyle(color = TextPrimary)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    )
}