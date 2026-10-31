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

// ---- Recent projects (SharedPreferences via AndroidContext) -----------------

object RecentProjectsStore {
    private const val FILE = "komet_ide_prefs"
    private const val KEY = "recent_projects"
    private const val SEP = "|"

    fun get(): List<String> {
        val prefs = AndroidContext.appContext
            ?.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            ?: return emptyList()
        val raw = prefs.getString(KEY, "") ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).filter { it.isNotBlank() }
    }

    fun save(paths: List<String>) {
        val prefs = AndroidContext.appContext
            ?.getSharedPreferences(FILE, android.content.Context.MODE_PRIVATE)
            ?: return
        prefs.edit().putString(KEY, paths.joinToString(SEP)).apply()
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

/** Prefers app-private storage; falls back to the app files dir. */
fun defaultProjectsDirectory(): String {
    val ctx = AndroidContext.appContext ?: return "/sdcard/KometIDEProjects"
    val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
    return File(base, "KometIDEProjects").absolutePath
}

// ---- Self update ------------------------------------------------------------

/** Android installs updates through the Play Store, so self-update is off. */
val platformSupportsSelfUpdate: Boolean = false

suspend fun downloadInstaller(
    url: String,
    onProgress: (downloaded: Long, total: Long) -> Unit
): String? = null

suspend fun installInstaller(path: String): Boolean = false

/** Android reads its own download entry from version.json. */
val platformUpdateUrlKey: String = "url_android"
