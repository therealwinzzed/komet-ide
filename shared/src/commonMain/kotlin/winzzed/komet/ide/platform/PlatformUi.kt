package winzzed.komet.ide.platform

/**
 * Directory picker. On desktop this wraps JFileChooser; on Android it launches the
 * system document-tree picker; on web it uses the File System Access API when available.
 *
 * [onResult] receives the chosen absolute path, or null when the user cancelled.
 * It may be invoked asynchronously, so callers must not assume it runs inline.
 */
expect fun pickDirectory(
    title: String,
    initialPath: String?,
    onResult: (String?) -> Unit
)

/** Save-file picker used by the "build plugin" action. */
expect fun pickSaveFile(
    title: String,
    suggestedName: String,
    onResult: (String?) -> Unit
)

/** Copies [text] to the system clipboard. No-op where unsupported. */
expect fun copyToClipboard(text: String)

/** Terminates the application where the platform allows it. */
expect fun exitApplication()

/** True when [pickDirectory] can actually show a native picker on this platform. */
expect val platformSupportsDirectoryPicker: Boolean

/** Human-readable temporary directory, or null when the platform has none. */
expect fun temporaryDirectory(): String?
