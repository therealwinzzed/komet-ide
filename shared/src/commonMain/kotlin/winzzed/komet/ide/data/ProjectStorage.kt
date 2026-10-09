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

    fun buildKinetZip(outPath: String): String? =
        buildZip(
            outPath,
            listFiles().filter { it.name != "Help.md" }.map { it.path }
        )

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

// ---- Platform primitives ----------------------------------------------------

/** True when [path] exists (file or directory). */
expect fun exists(path: String): Boolean

/** True when [path] exists and is a regular file. */
expect fun isFile(path: String): Boolean

/** Entry names inside [dirPath]; empty when missing or unreadable. */
expect fun listNames(dirPath: String): List<String>

/** Reads a text file as UTF-8. Throws when unreadable. */
expect fun readRaw(path: String): String

/** Writes a text file as UTF-8, creating parent directories. Throws on failure. */
expect fun writeRaw(path: String, content: String)

/** Copies a file, overwriting [to]. Throws on failure. */
expect fun copyRaw(from: String, to: String)

/** Writes [entries] into a zip at [outPath]; returns outPath, or null on failure. */
expect fun buildZip(outPath: String, entries: List<String>): String?

/** Default parent directory for new projects, e.g. ~/KometIDEProjects. */
expect fun defaultProjectsDirectory(): String

/** Whether the given path exists on disk. Always false on web. */
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
