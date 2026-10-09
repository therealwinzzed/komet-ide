package winzzed.komet.ide.data

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSMutableURLRequest
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequestReloadIgnoringLocalCacheData
import platform.Foundation.NSURLResponse
import platform.Foundation.NSURLSession
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataTaskWithRequest
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// ---- HTTP -------------------------------------------------------------------

actual val platformSupportsNetwork: Boolean = true

/**
 * Fetches [url] with NSURLSession. The callback is bridged into a coroutine so
 * callers in common code see a normal suspend function.
 */
@OptIn(ExperimentalForeignApi::class)
actual suspend fun httpGetText(url: String, timeoutMs: Int): String? {
    val nsUrl = NSURL.URLWithString(url) ?: return null
    val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
        setTimeoutInterval(timeoutMs.toDouble() / 1000.0)
        setCachePolicy(NSURLRequestReloadIgnoringLocalCacheData)
    }

    return suspendCoroutine { cont ->
        val task = NSURLSession.sharedSession.dataTaskWithRequest(request) { data, response, error ->
            val http = response as? NSHTTPURLResponse
            val ok = error == null && (http == null || http.statusCode in 200..299)
            if (!ok) {
                cont.resume(null)
                return@dataTaskWithRequest
            }
            val nsString = data?.let { NSString.create(it, NSUTF8StringEncoding) }
            cont.resume(nsString as String?)
        }
        task.resume()
    }
}

// ---- Recent projects --------------------------------------------------------

actual object RecentProjectsStore {
    private const val KEY = "recent_projects"
    private const val SEP = "|"

    actual fun get(): List<String> {
        val raw = NSUserDefaults.standardUserDefaults.stringForKey(KEY) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEP).filter { it.isNotBlank() }
    }

    actual fun save(paths: List<String>) {
        NSUserDefaults.standardUserDefaults.setObject(paths.joinToString(SEP), KEY)
    }
}

// ---- Filesystem primitives --------------------------------------------------

@OptIn(ExperimentalForeignApi::class)
private fun fileManager(): NSFileManager = NSFileManager.defaultManager

actual fun exists(path: String): Boolean = fileManager().fileExistsAtPath(path)

actual fun isFile(path: String): Boolean {
    var isDir = false
    val exists = memScoped {
        val ref = alloc<BooleanVar>()
        ref.value = false
        val ok = fileManager().fileExistsAtPath(path, ref.ptr)
        isDir = ref.value
        ok
    }
    return exists && !isDir
}

actual fun listNames(dirPath: String): List<String> {
    val contents = fileManager().contentsOfDirectoryAtPath(dirPath, null) ?: return emptyList()
    return contents.filterIsInstance<String>()
}

actual fun readRaw(path: String): String =
    NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null) as String?
        ?: throw IllegalStateException("Cannot read $path")

actual fun writeRaw(path: String, content: String) {
    val parent = parentPathOf(path)
    if (parent != null && !fileManager().fileExistsAtPath(parent)) {
        fileManager().createDirectoryAtPath(parent, true, null, null)
    }
    val nsContent = NSString.create(string = content)
    val ok = nsContent.writeToFile(path, true, NSUTF8StringEncoding, null)
    if (!ok) throw IllegalStateException("Cannot write $path")
}

actual fun copyRaw(from: String, to: String) {
    val parent = parentPathOf(to)
    if (parent != null && !fileManager().fileExistsAtPath(parent)) {
        fileManager().createDirectoryAtPath(parent, true, null, null)
    }
    if (fileManager().fileExistsAtPath(to)) fileManager().removeItemAtPath(to, null)
    fileManager().copyItemAtPath(from, to, null)
}

/** No zip support in the iOS common layer; callers degrade gracefully. */
actual fun buildZip(outPath: String, entries: List<String>): String? = null

/** App sandbox Documents directory. */
@OptIn(ExperimentalForeignApi::class)
actual fun defaultProjectsDirectory(): String {
    val docs = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory,
        NSUserDomainMask,
        true
    ).firstOrNull() as? String ?: NSTemporaryDirectory()
    return "$docs/KometIDEProjects"
}

// ---- Self update ------------------------------------------------------------

/** iOS updates ship through the App Store. */
actual val platformSupportsSelfUpdate: Boolean = false

actual suspend fun downloadInstaller(
    url: String,
    onProgress: (downloaded: Long, total: Long) -> Unit
): String? = null

actual suspend fun installInstaller(path: String): Boolean = false
