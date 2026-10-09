package winzzed.komet.ide.data

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// ---- Browser interop --------------------------------------------------------
// Declared as external so the same source shape compiles for both JS and WasmJS
// (Kotlin/Wasm has no `dynamic` type).

private external fun fetchText(url: String): Promise

private external fun localStorageGetItem(key: String): String?

private external fun localStorageSetItem(key: String, value: String)

/**
 * Kotlin/Wasm interop only allows primitive/string/function types, so the promise
 * is modelled with a non-generic callback signature that both targets accept.
 */
private external interface Promise {
    fun then(onFulfilled: (String) -> Unit, onRejected: (String) -> Unit)
}

private external fun clipboardWriteText(text: String)

// ---- HTTP -------------------------------------------------------------------

actual val platformSupportsNetwork: Boolean = true

actual suspend fun httpGetText(url: String, timeoutMs: Int): String? =
    suspendCancellableCoroutine { cont ->
        try {
            fetchText(url).then(
                onFulfilled = { text -> cont.resume(text) },
                onRejected = { _ -> cont.resume(null) }
            )        } catch (_: Throwable) {
            cont.resume(null)
        }
    }

// ---- Recent projects (localStorage) -----------------------------------------

actual object RecentProjectsStore {
    private const val KEY = "komet_ide_recent_projects"
    private const val SEP = "|"

    actual fun get(): List<String> = try {
        val raw = localStorageGetItem(KEY)
        if (raw.isNullOrBlank()) emptyList()
        else raw.split(SEP).filter { it.isNotBlank() }
    } catch (_: Throwable) {
        emptyList()
    }

    actual fun save(paths: List<String>) {
        try {
            localStorageSetItem(KEY, paths.joinToString(SEP))
        } catch (_: Throwable) {
        }
    }
}

// ---- Filesystem -------------------------------------------------------------
// A browser has no real filesystem. Projects live in an in-memory store so the UI
// is fully usable in the browser; nothing persists across a page reload.

private val memoryFiles = mutableMapOf<String, String>()

actual fun exists(path: String): Boolean = memoryFiles.containsKey(path)

actual fun isFile(path: String): Boolean = memoryFiles.containsKey(path)

actual fun listNames(dirPath: String): List<String> {
    val prefix = dirPath.trimEnd('/') + "/"
    return memoryFiles.keys
        .filter { it.startsWith(prefix) }
        .map { it.removePrefix(prefix) }
        .filter { it.isNotEmpty() && !it.contains('/') }
}

actual fun readRaw(path: String): String =
    memoryFiles[path] ?: throw IllegalStateException("No such file: $path")

actual fun writeRaw(path: String, content: String) {
    memoryFiles[path] = content
}

actual fun copyRaw(from: String, to: String) {
    val content = memoryFiles[from] ?: throw IllegalStateException("No such file: $from")
    memoryFiles[to] = content
}

/** No zip in the browser; callers show an error instead. */
actual fun buildZip(outPath: String, entries: List<String>): String? = null

actual fun defaultProjectsDirectory(): String = "/KometIDEProjects"

// ---- Self update ------------------------------------------------------------

actual val platformSupportsSelfUpdate: Boolean = false

actual suspend fun downloadInstaller(
    url: String,
    onProgress: (downloaded: Long, total: Long) -> Unit
): String? = null

actual suspend fun installInstaller(path: String): Boolean = false
