package winzzed.komet.ide.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class UpdateInfo(
    val version: String,
    val description: String,
    val url: String
)

/**
 * The self-update flow is only meaningful on desktop (it downloads and launches a new
 * .exe/.msi installer). Other platforms only report that an update exists.
 */
expect val platformSupportsSelfUpdate: Boolean

object UpdateChecker {
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.Default) {
        if (!platformSupportsNetwork) return@withContext null
        try {
            val json = httpGetText(version_json, timeoutMs = 5_000) ?: return@withContext null

            val version = extractJsonString(json, "version") ?: return@withContext null
            val description = extractJsonString(json, "description") ?: ""
            val url = extractJsonString(json, "url") ?: return@withContext null

            if (isNewer(version, CURRENT_VERSION)) {
                UpdateInfo(version, description, url)
            } else null
        } catch (t: Throwable) {
            println("UpdateChecker: check failed: ${t.message}")
            null
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val regex = """"$key"\s*:\s*"([^"]*)"""".toRegex()
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun isNewer(remote: String, local: String): Boolean {
        val r = remote.split(".").map { it.toIntOrNull() ?: 0 }
        val l = local.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(r.size, l.size)) {
            val rv = r.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (rv > lv) return true
            if (rv < lv) return false
        }
        return false
    }

    /**
     * Downloads the installer. Returns the resulting file path, or null when the
     * platform cannot perform self-update.
     *
     * [onProgress] is called with (downloadedBytes, totalBytes); totalBytes is -1
     * when the server does not report a content length.
     */
    suspend fun download(
        url: String,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ): String? = withContext(Dispatchers.Default) {
        if (!platformSupportsSelfUpdate) return@withContext null
        try {
            downloadInstaller(url, onProgress)
        } catch (t: Throwable) {
            println("UpdateChecker: download failed: ${t.message}")
            null
        }
    }

    /** Launches the downloaded installer, then the caller usually restarts the app. */
    suspend fun install(path: String): Boolean =
        if (!platformSupportsSelfUpdate) false else installInstaller(path)
}

/** Platform hook: performs the actual download. */
expect suspend fun downloadInstaller(
    url: String,
    onProgress: (downloaded: Long, total: Long) -> Unit
): String?

/** Platform hook: launches the downloaded installer. */
expect suspend fun installInstaller(path: String): Boolean
