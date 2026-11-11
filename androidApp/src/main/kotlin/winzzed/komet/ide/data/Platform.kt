package winzzed.komet.ide.data

import android.os.Environment
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

fun writeBytes(path: String, bytes: ByteArray) {
    val f = File(path)
    f.parentFile?.mkdirs()
    f.writeBytes(bytes)
}

fun readBytes(path: String): ByteArray = File(path).readBytes()

fun mkdirs(path: String): Boolean = File(path).let { it.exists() || it.mkdirs() }

/**
 * Decompresses a raw DEFLATE stream from a ZIP entry.
 *
 * ZIP stores raw DEFLATE without the zlib wrapper, so [java.util.zip.Inflater] must
 * be created with nowrap = true; otherwise it expects a zlib header and fails.
 */
fun inflate(raw: ByteArray): ByteArray {
    val inflater = java.util.zip.Inflater(true)
    return try {
        inflater.setInput(raw)
        val out = java.io.ByteArrayOutputStream(raw.size.coerceAtLeast(64) * 2)
        val chunk = ByteArray(8 * 1024)
        while (true) {
            val n = try {
                inflater.inflate(chunk)
            } catch (_: java.util.zip.DataFormatException) {
                break
            }
            if (n > 0) out.write(chunk, 0, n) else break
        }
        out.toByteArray()
    } finally {
        inflater.end()
    }
}

/**
 * The shared projects root: `/storage/emulated/0/KometIDEProjects`.
 *
 * This is the public internal-storage folder, so projects survive an app reinstall
 * and are visible in file managers. Writing there needs scoped-storage access; see
 * [ensureProjectsDirectory] for the fallback chain.
 */
fun defaultProjectsDirectory(): String =
    Environment.getExternalStorageDirectory().absolutePath + "/KometIDEProjects"

/**
 * Returns a writable projects root.
 *
 * On Android 11+ an app cannot create arbitrary folders in shared storage. Either
 * the folder already exists and is writable (older install, or created by the user),
 * or the user granted access to it through the system picker (SAF tree). Otherwise
 * this falls back to app-specific external storage, which never needs a prompt.
 */
fun ensureProjectsDirectory(): String {
    val shared = defaultProjectsDirectory()
    val direct = File(shared)
    if ((direct.isDirectory || direct.mkdirs()) && direct.canWrite()) return shared

    ScopedStorage.projectsTreeUri()?.let { uri ->
        if (ScopedStorage.isWritable(uri)) return shared
    }

    val ctx = AndroidContext.appContext ?: return shared
    val base = ctx.getExternalFilesDir(null) ?: ctx.filesDir
    return File(base, "KometIDEProjects").apply { mkdirs() }.absolutePath
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
