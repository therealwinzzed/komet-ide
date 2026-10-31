package winzzed.komet.ide.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

private val COLOR_KEY = Color(0xFF9CDCFE)
private val COLOR_STRING = Color(0xFFCE9178)
private val COLOR_NUMBER = Color(0xFFB5CEA8)
private val COLOR_KEYWORD = Color(0xFFC586C0)
private val COLOR_COMMENT = Color(0xFF6A9955)
private val COLOR_PUNCTUATION = Color(0xFFD4D4D4)

class JsonSyntaxHighlight : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val builder = AnnotatedString.Builder(original)

        val keyRegex = """"([^"]+)"\s*:""".toRegex()
        val stringRegex = """"([^"]*)"""".toRegex()
        val numberRegex = """\b-?\d+(\.\d+)?\b""".toRegex()

        keyRegex.findAll(original).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = COLOR_KEY, fontWeight = FontWeight.Bold), start, end)
        }

        stringRegex.findAll(original).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            if (!builder.toAnnotatedString().spanStyles.any { it.start <= start && it.end >= end }) {
                builder.addStyle(SpanStyle(color = COLOR_STRING), start, end)
            }
        }

        numberRegex.findAll(original).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1
            builder.addStyle(SpanStyle(color = COLOR_NUMBER), start, end)
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

class JavaScriptSyntaxHighlight : VisualTransformation {
    private val keywords = setOf(
        "function", "return", "const", "let", "var", "if", "else",
        "for", "while", "true", "false", "null", "undefined", "new",
        "this", "import", "async", "export"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val builder = AnnotatedString.Builder(original)

        val keywordRegex = """\b(${keywords.joinToString("|")})\b""".toRegex()
        keywordRegex.findAll(original).forEach { match ->
            builder.addStyle(
                SpanStyle(color = COLOR_KEYWORD, fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        val stringRegex = """(['"])(?:(?!\1).)*\1""".toRegex()
        stringRegex.findAll(original).forEach { match ->
            builder.addStyle(
                SpanStyle(color = COLOR_STRING),
                match.range.first,
                match.range.last + 1
            )
        }

        val numberRegex = """\b-?\d+(\.\d+)?\b""".toRegex()
        numberRegex.findAll(original).forEach { match ->
            builder.addStyle(
                SpanStyle(color = COLOR_NUMBER),
                match.range.first,
                match.range.last + 1
            )
        }

        val lineCommentRegex = """//.*""".toRegex()
        lineCommentRegex.findAll(original).forEach { match ->
            builder.addStyle(
                SpanStyle(color = COLOR_COMMENT),
                match.range.first,
                match.range.last + 1
            )
        }

        val blockCommentRegex = """/\*[\s\S]*?\*/""".toRegex()
        blockCommentRegex.findAll(original).forEach { match ->
            builder.addStyle(
                SpanStyle(color = COLOR_COMMENT),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}