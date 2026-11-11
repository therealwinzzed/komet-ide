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

/**
 * Unpacks a `.kinet` archive into a new project folder under [parentLocation] so it
 * can be edited and compiled again.
 *
 * The folder is named after the plugin's manifest `name` when present, otherwise
 * after the archive file name. An existing folder with that name is reused rather
 * than duplicated, so re-importing an archive updates the project in place.
 */
fun importKinetProject(
    archiveBytes: ByteArray,
    archiveName: String,
    parentLocation: String
): ImportResult {
    val entries = try {
        ZipReader.extract(archiveBytes) { raw -> inflate(raw) }
    } catch (t: Throwable) {
        return ImportResult(false, message = "Архив повреждён или использует неподдерживаемое сжатие")
    } ?: return ImportResult(false, message = "Это не похоже на плагин .kinet")

    if (entries.isEmpty()) {
        return ImportResult(false, message = "Архив пуст")
    }

    // Prefer the manifest name so the folder matches what the plugin calls itself.
    val manifest = entries.firstOrNull { it.first.substringAfterLast('/') == "manifest.json" }
        ?.second?.decodeToString()
    val pluginName = manifest?.let { parsePluginInfo(it).name }
        ?.takeIf { it.isNotBlank() && it != "Плагин" }
    val fallbackName = archiveName.substringAfterLast('/').substringAfterLast('\\')
        .substringBeforeLast('.')
    val folderName = sanitizeFolderName(pluginName ?: fallbackName)

    val projectPath = joinPath(parentLocation, folderName)
    val storage = ProjectStorage(projectPath)
    mkdirs(projectPath)

    val written = try {
        storage.importArchiveEntries(entries)
    } catch (t: Throwable) {
        return ImportResult(false, message = t.message ?: "Не удалось записать файлы")
    }

    if (written <= 0) {
        return ImportResult(false, message = "Архив не содержит файлов")
    }

    return ImportResult(ok = true, projectPath = projectPath, fileCount = written)
}

/** Writes already-extracted archive entries into this project. */
private fun ProjectStorage.importArchiveEntries(
    entries: List<Pair<String, ByteArray>>
): Int {
    var written = 0
    for ((rawName, content) in entries) {
        // Reject absolute paths and ".." so an archive cannot escape the folder.
        val name = rawName.replace('\\', '/').trimStart('/')
        if (name.isEmpty() || name.split('/').any { it == ".." }) continue

        val target = joinPath(basePath, name)
        if (name.contains('/')) mkdirs(target.substringBeforeLast('/'))
        writeBytes(target, content)
        written++
    }
    return written
}

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
