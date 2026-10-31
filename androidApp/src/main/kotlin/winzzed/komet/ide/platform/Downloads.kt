package winzzed.komet.ide.platform

import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import winzzed.komet.ide.data.AndroidContext
import java.io.File

/** Outcome of saving a compiled plugin to the platform's download location. */
data class DownloadResult(
    val ok: Boolean,
    val location: String? = null,
    val message: String? = null
)

val downloadsFolderLabel: String = "Downloads"

/**
 * Android 10 (API 29) introduced scoped storage. From that release MediaStore lets an
 * app publish a file into the shared Downloads collection with no permission at all,
 * which is what most devices run today.
 *
 * On API 24-28 the public folder is only writable with WRITE_EXTERNAL_STORAGE, a
 * runtime permission. Rather than fail silently we try the direct write and, when it
 * is denied, fall back to app storage and say so.
 */
suspend fun saveToDownloads(fileName: String, bytes: ByteArray): DownloadResult =
    withContext(Dispatchers.IO) {
        // The zip writer always emits at least an end-of-directory record, so an
        // empty array means something failed upstream.
        if (bytes.isEmpty()) {
            return@withContext DownloadResult(false, message = "Архив пуст")
        }

        val context = AndroidContext.appContext
            ?: return@withContext DownloadResult(false, message = "Контекст Android недоступен")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, fileName, bytes)
        } else {
            saveViaPublicDirectory(context, fileName, bytes)
        }
    }

/**
 * MIME type used when publishing to Downloads.
 *
 * Android's MediaProvider reconciles the file extension with the MIME type: when
 * they disagree it replaces the extension with the MIME-derived one. Declaring
 * `application/zip` for `plugin.kinet` therefore produced `plugin.zip`, because
 * `.kinet` is not a known extension and the fallback type for unknown extensions is
 * exactly `application/octet-stream`. Using that same generic type makes the
 * condition match, so the `.kinet` name is preserved.
 */
internal const val DOWNLOAD_MIME_TYPE = "application/octet-stream"

/** Values MediaStore is given for a new download; kept separate so it can be tested. */
internal fun downloadValues(fileName: String, pending: Boolean): ContentValues =
    ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, DOWNLOAD_MIME_TYPE)
        put(MediaStore.MediaColumns.IS_PENDING, if (pending) 1 else 0)
    }

private fun saveViaMediaStore(
    context: android.content.Context,
    fileName: String,
    bytes: ByteArray
): DownloadResult {
    val resolver = context.contentResolver
    val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI

    return try {
        val uri = resolver.insert(collection, downloadValues(fileName, pending = true))
            ?: return DownloadResult(false, message = "MediaStore отклонил запись")

        try {
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return DownloadResult(false, message = "Не удалось открыть поток записи")
        } finally {
            // Clear the pending flag so the file becomes visible to other apps.
            resolver.update(uri, downloadValues(fileName, pending = false), null, null)
        }

        // Trust but verify: if the platform still rewrote the name, put it right and
        // report what actually ended up on disk rather than what we asked for.
        val stored = queryDisplayName(resolver, uri)
        if (stored != null && stored != fileName) {
            runCatching {
                val fix = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                }
                resolver.update(uri, fix, null, null)
            }
        }
        val finalName = queryDisplayName(resolver, uri) ?: stored ?: fileName

        DownloadResult(
            ok = true,
            location = "Downloads/$finalName",
            message = if (finalName != fileName) {
                "Система сохранила файл как $finalName"
            } else null
        )
    } catch (t: Throwable) {
        DownloadResult(false, message = t.message ?: "Не удалось сохранить в Downloads")
    }
}

/** Reads DISPLAY_NAME back from MediaStore, or null when it cannot be queried. */
private fun queryDisplayName(
    resolver: android.content.ContentResolver,
    uri: android.net.Uri
): String? = try {
    resolver.query(
        uri,
        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    }
} catch (_: Throwable) {
    null
}

private fun saveViaPublicDirectory(
    context: android.content.Context,
    fileName: String,
    bytes: ByteArray
): DownloadResult {
    // Legacy path. Works only when WRITE_EXTERNAL_STORAGE has been granted.
    return try {
        @Suppress("DEPRECATION")
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        if (!downloads.exists()) downloads.mkdirs()
        val target = File(downloads, fileName)
        target.writeBytes(bytes)
        DownloadResult(true, location = target.absolutePath)
    } catch (t: Throwable) {
        // Fall back to app storage so the archive is never lost.
        try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val target = File(dir, fileName)
            target.writeBytes(bytes)
            DownloadResult(
                ok = true,
                location = target.absolutePath,
                message = "Нет доступа к общей папке Downloads на этой версии Android, " +
                    "файл сохранён в папку приложения"
            )
        } catch (t2: Throwable) {
            DownloadResult(false, message = t2.message ?: "Не удалось сохранить файл")
        }
    }
}
