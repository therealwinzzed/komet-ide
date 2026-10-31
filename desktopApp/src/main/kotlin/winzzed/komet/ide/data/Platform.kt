package winzzed.komet.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

// ---- HTTP -------------------------------------------------------------------

val platformSupportsNetwork: Boolean = true

val platformSupportsUpdateCheck: Boolean = true

suspend fun httpGetText(url: String, timeoutMs: Int): String? =
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

object RecentProjectsStore {
    private const val KEY = "recent_projects"
    private const val SEP = "|"

    private val prefs = java.util.prefs.Preferences.userRoot().node("winzzed/komet/ide")

    fun get(): List<String> {
        val raw = prefs.get(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).filter { it.isNotBlank() }
    }

    fun save(paths: List<String>) {
        prefs.put(KEY, paths.joinToString(SEP))
    }
}

// ---- Filesystem primitives --------------------------------------------------

fun exists(path: String): Boolean = File(path).exists()

fun isFile(path: String): Boolean = File(path).isFile

fun isDirectory(path: String): Boolean = File(path).isDirectory

fun listNames(dirPath: String): List<String> =
    File(dirPath).listFiles()?.map { it.name } ?: emptyList()

fun readRaw(path: String): String = File(path).readText()

fun writeRaw(path: String, content: String) {
    val f = File(path)
    f.parentFile?.mkdirs()
    f.writeText(content)
}

fun copyRaw(from: String, to: String) {
    val target = File(to)
    target.parentFile?.mkdirs()
    File(from).copyTo(target, overwrite = true)
}

fun defaultProjectsDirectory(): String =
    File(System.getProperty("user.home") ?: ".", "KometIDEProjects").absolutePath

// ---- Self update ------------------------------------------------------------

val platformSupportsSelfUpdate: Boolean = true

suspend fun downloadInstaller(
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

suspend fun installInstaller(path: String): Boolean = withContext(Dispatchers.IO) {
    try {
        val file = File(path)
        if (!file.exists()) return@withContext false
        ProcessBuilder(file.absolutePath).directory(file.parentFile).start()
        true
    } catch (_: Throwable) {
        false
    }
}

/** Desktop reads the PC/desktop download entry from version.json. */
val platformUpdateUrlKey: String = "url_pc"
