package winzzed.komet.ide.viewmodel

/** iOS ships no bundled Help.md resource yet, so use the inline fallback. */
actual fun bundledHelpText(): String = IdeViewModel.DEFAULT_HELP
