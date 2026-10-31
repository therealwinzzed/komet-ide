package winzzed.komet.ide.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Outcome of saving a compiled plugin to the platform's download location. */
data class DownloadResult(
    val ok: Boolean,
    val location: String? = null,
    val message: String? = null
)

val downloadsFolderLabel: String = "Downloads"

suspend fun saveToDownloads(fileName: String, bytes: ByteArray): DownloadResult =
    withContext(Dispatchers.IO) {
        // The zip writer always emits at least an end-of-directory record, so an
        // empty array means something failed upstream.
        if (bytes.isEmpty()) {
            return@withContext DownloadResult(false, message = "Архив пуст")
        }

        val home = System.getProperty("user.home")
            ?: return@withContext DownloadResult(false, message = "Не удалось определить домашний каталог")

        val downloads = File(home, "Downloads")
        val target = File(downloads, fileName)

        try {
            if (!downloads.exists()) downloads.mkdirs()
            target.writeBytes(bytes)
            DownloadResult(true, location = target.absolutePath)
        } catch (t: Throwable) {
            // ~/Downloads may be missing or read-only; the home directory still works.
            try {
                val fallback = File(home, fileName)
                fallback.writeBytes(bytes)
                DownloadResult(
                    ok = true,
                    location = fallback.absolutePath,
                    message = "Папка Downloads недоступна, файл сохранён в домашний каталог"
                )
            } catch (t2: Throwable) {
                DownloadResult(
                    false,
                    message = t2.message ?: "Не удалось сохранить файл"
                )
            }
        }
    }
