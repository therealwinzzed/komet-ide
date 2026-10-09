package winzzed.komet.ide.viewmodel

import java.io.File

/**
 * Reads the bundled Help.md. The desktop distribution ships Help.md on the
 * classpath root; if it is missing we fall back to the resource on disk, then
 * to a minimal inline string.
 */
actual fun bundledHelpText(): String {
    return try {
        val stream = object {}.javaClass.getResourceAsStream("/Help.md")
        if (stream != null) {
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } else {
            fallbackFromDisk()
        }
    } catch (_: Throwable) {
        IdeViewModel.DEFAULT_HELP
    }
}

private fun fallbackFromDisk(): String {
    return try {
        // Walk up from the working directory looking for the module resource.
        val candidates = listOf(
            "desktopApp/src/main/resources/Help.md",
            "Help.md"
        )
        val found = candidates.map { File(it) }.firstOrNull { it.isFile }
        found?.readText() ?: IdeViewModel.DEFAULT_HELP
    } catch (_: Throwable) {
        IdeViewModel.DEFAULT_HELP
    }
}
