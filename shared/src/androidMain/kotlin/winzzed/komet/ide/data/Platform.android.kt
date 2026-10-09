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

// ---- Recent projects (SharedPreferences via AndroidContext) -----------------

actual object RecentProjectsStore {
    private const val FILE = "komet_ide_prefs"
    private const val KEY = "recent_projects"
    private const val SEP = "|"

    actual fun get(): List<String> {
        val prefs = AndroidContext.appContext
            ?.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            ?: return emptyList()
        val raw = prefs.getString(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).filter { it.isNotBlank() }
    }

    actual fun save(paths: List<String>) {
        val prefs = AndroidContext.appContext
            ?.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            ?: return
        prefs.edit().putString(KEY, paths.joinToString(SEP)).apply()
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

/** Prefers app-private storage; falls back to the app files dir. */
actual fun defaultProjectsDirectory(): String {
    val ctx = AndroidContext.appContext ?: return "/sdcard/KometIDEProjects"
    val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
    return File(base, "KometIDEProjects").absolutePath
}

// ---- Self update ------------------------------------------------------------

/** Android installs updates through the Play Store, so self-update is off. */
actual val platformSupportsSelfUpdate: Boolean = false

actual suspend fun downloadInstaller(
    url: String,
    onProgress: (downloaded: Long, total: Long) -> Unit
): String? = null

actual suspend fun installInstaller(path: String): Boolean = false
