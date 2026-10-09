package winzzed.komet.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// ---- HTTP -------------------------------------------------------------------

actual val platformSupportsNetwork: Boolean = true

actual suspend fun httpGetText(url: String, timeoutMs: Int): String? =
    withContext(Dispatchers.IO) {
        try {
            val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
            }
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                .also { conn.disconnect() }
        } catch (_: Throwable) {
            null
        }
    }

// ---- Recent projects --------------------------------------------------------

actual object RecentProjectsStore {
    private const val KEY = "recent_projects"
    private const val SEP = "|"

    private val prefs = java.util.prefs.Preferences.userRoot().node("winzzed/komet/ide")

    actual fun get(): List<String> {
        val raw = prefs.get(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).filter { it.isNotBlank() }
    }

    actual fun save(paths: List<String>) {
        prefs.put(KEY, paths.joinToString(SEP))
    }
}

// ---- Filesystem primitives --------------------------------------------------

actual fun exists(path: String): Boolean = File(path).exists()

actual fun isFile(path: String): Boolean = File(path).isFile

actual fun listNames(dirPath: String): List<String> =
    File(dirPath).listFiles()?.map { it.name } ?: emptyList()

actual fun readRaw(path: String): String = File(path).readText()

actual fun writeRaw(path: String, content: String) {
    val f = File(path)
    f.parentFile?.mkdirs()
    f.writeText(content)
}

actual fun copyRaw(from: String, to: String) {
    val target = File(to)
    target.parentFile?.mkdirs()
    File(from).copyTo(target, overwrite = true)
}

actual fun buildZip(outPath: String, entries: List<String>): String? = try {
    val out = File(outPath)
    out.parentFile?.mkdirs()
    ZipOutputStream(out.outputStream().buffered()).use { zip ->
        for (path in entries) {
            val f = File(path)
            if (!f.isFile) continue
            zip.putNextEntry(ZipEntry(f.name))
            f.inputStream().use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }
    outPath
} catch (_: Throwable) {
    null
}

actual fun defaultProjectsDirectory(): String =
    File(System.getProperty("user.home") ?: ".", "KometIDEProjects").absolutePath

// ---- Self update ------------------------------------------------------------

actual val platformSupportsSelfUpdate: Boolean = true

actual suspend fun downloadInstaller(
    url: String,
    onProgress: (downloaded: Long, total: Long) -> Unit
): String? = withContext(Dispatchers.IO) {
    val userHome = System.getProperty("user.home") ?: "."
    val downloads = File(userHome, "KometIDE_Updates").apply { mkdirs() }

    val targetFile = File(downloads, "KometIDE_${System.currentTimeMillis()}.exe")

    downloads.listFiles()?.forEach { old ->
        if (old.name != targetFile.name) {
            try { old.delete() } catch (_: Exception) { }
        }
    }

    val conn = (URI(url).toURL().openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 120_000
        requestMethod = "GET"
    }

    val total = conn.contentLengthLong
    var downloaded = 0L

    try {
        conn.inputStream.use { input ->
            java.io.FileOutputStream(targetFile).use { output ->
                val buffer = ByteArray(8192)
                var bytes: Int
                while (input.read(buffer).also { bytes = it } != -1) {
                    output.write(buffer, 0, bytes)
                    downloaded += bytes
                    onProgress(downloaded, total)
                }
            }
        }
    } finally {
        conn.disconnect()
    }

    targetFile.absolutePath
}

actual suspend fun installInstaller(path: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val file = File(path)
        if (!file.exists()) return@withContext false
        ProcessBuilder(file.absolutePath).directory(file.parentFile).start()
        true
    } catch (_: Throwable) {
        false
    }
}
