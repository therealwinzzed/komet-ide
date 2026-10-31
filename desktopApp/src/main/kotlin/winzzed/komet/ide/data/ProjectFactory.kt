package winzzed.komet.ide.data

/**
 * Creates a new project as its own sub-folder of [parentLocation] and returns the
 * created project's absolute path.
 *
 * Each project gets a dedicated directory (like IntelliJ/Android Studio), otherwise
 * creating a second project would overwrite the first one in the same folder.
 */
fun createProjectOnDisk(
    name: String,
    packageName: String,
    parentLocation: String
): String {
    val folderName = sanitizeFolderName(name)
    val projectPath = joinPath(parentLocation, folderName)

    val storage = ProjectStorage(projectPath)
    storage.ensureSkeleton()
    storage.writeManifest(
        """
        {
          "name": "${escapeJson(name)}",
          "package": "${escapeJson(packageName)}",
          "version": "1.0.0"
        }
        """.trimIndent()
    )
    return projectPath
}

/** Joins path segments without doubling separators. */
fun joinPath(parent: String, child: String): String =
    parent.trimEnd('/', '\\') + "/" + child.trimStart('/', '\\')

/** Strips characters that are illegal in folder names on common filesystems. */
fun sanitizeFolderName(name: String): String {
    val cleaned = name.trim()
        .map { c -> if (c in ILLEGAL_FOLDER_CHARS) '_' else c }
        .joinToString("")
        .trim('.', ' ')
    return cleaned.ifBlank { "Untitled" }
}

private val ILLEGAL_FOLDER_CHARS = charArrayOf(
    '/', '\\', ':', '*', '?', '"', '<', '>', '|'
)

/** Escapes the two characters that would break a JSON string literal. */
private fun escapeJson(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")
