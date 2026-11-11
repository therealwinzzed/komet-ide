package winzzed.komet.ide.data

/**
 * Platform-agnostic view of a project file. Holds only the metadata the UI needs;
 * the actual I/O is delegated to [ProjectStorage].
 */
data class ProjectFile(
    val name: String,
    val path: String
)

/**
 * Filesystem + project operations. Implemented per platform because desktop/Android use
 * java.io.File, iOS uses NSFileManager and the web targets have no real filesystem.
 */
class ProjectStorage(val basePath: String) {

    fun ensureSkeleton() {
        ensureFile("manifest.json", "{\n  \n}\n")
        ensureFile("main.js", "// main.js\n")
    }

    private fun ensureFile(name: String, default: String) {
        if (!exists("$basePath/$name")) writeRaw("$basePath/$name", default)
    }

    fun listFiles(): List<ProjectFile> =
        listNames(basePath)
            .filter { isFile("$basePath/$it") }
            .sorted()
            .map { ProjectFile(it, "$basePath/$it") }

    fun read(file: ProjectFile): String =
        try { if (exists(file.path)) readRaw(file.path) else "" } catch (_: Throwable) { "" }

    fun write(file: ProjectFile, content: String) {
        try { writeRaw(file.path, content) } catch (_: Throwable) { }
    }

    fun writeHelpIfMissing(text: String): Boolean {
        val helpPath = "$basePath/Help.md"
        if (exists(helpPath)) return false
        writeRaw(helpPath, text)
        return true
    }

    fun create(name: String): ProjectFile {
        val path = "$basePath/$name"
        if (!exists(path)) writeRaw(path, "")
        return ProjectFile(name, path)
    }

    /** Overwrites manifest.json with [text]. */
    fun writeManifest(text: String) {
        writeRaw("$basePath/manifest.json", text)
    }

    /** Project files that belong in the compiled archive (Help.md is not shipped). */
    fun archiveEntries(): List<ProjectFile> =
        listFiles().filter { it.name != "Help.md" }

    /**
     * Compiles the project into a plugin archive. Built in memory so every platform
     * can produce it, including iOS and the web targets which have no java.util.zip.
     */
    fun buildKinetZipBytes(): ByteArray? = try {
        ZipWriter.build(
            archiveEntries().map { it.name to readRaw(it.path).encodeToByteArray() }
        )
    } catch (_: Throwable) {
        null
    }

    companion object {
        /** Copies [from] to [to], returning true on success. */
        fun copyFile(from: String, to: String): Boolean {
            return try {
                copyRaw(from, to)
                true
            } catch (_: Throwable) {
                false
            }
        }
    }
}
/** Whether the given path exists on disk. */
fun pathExists(path: String): Boolean = exists(path)

/** Last path segment, without touching the real filesystem. */
fun fileNameOf(path: String): String =
    path.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\')

/** Parent path, without touching the real filesystem. */
fun parentPathOf(path: String): String? {
    val trimmed = path.trimEnd('/', '\\')
    val idx = trimmed.indexOfLast { it == '/' || it == '\\' }
    return if (idx <= 0) null else trimmed.substring(0, idx)
}

/** Sub-directory names inside [dirPath], sorted; empty when missing or unreadable. */
fun listDirectories(dirPath: String): List<String> =
    listNames(dirPath)
        .filter { isDirectory("$dirPath/$it") }
        .sorted()

/** True for a path that looks like a plugin archive. */
fun isKinetFile(path: String): Boolean =
    path.substringAfterLast('/', "").substringAfterLast('\\', "")
        .endsWith(".kinet", ignoreCase = true)