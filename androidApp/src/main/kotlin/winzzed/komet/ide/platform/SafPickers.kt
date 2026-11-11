package winzzed.komet.ide.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import winzzed.komet.ide.data.AndroidContext
import winzzed.komet.ide.data.PickedLocation
import winzzed.komet.ide.data.ScopedStorage

/**
 * The SAF pickers used by the IDE, created once per composition.
 *
 * Each launcher keeps its own pending callback so an activity recreation (rotation,
 * low memory) cannot deliver a result to the wrong request.
 */
class SafPickers internal constructor(
    private val launchDirectoryImpl: (onResult: (PickedLocation?) -> Unit) -> Unit,
    private val launchKinetFileImpl: (onResult: (Uri?) -> Unit) -> Unit,
    private val launchProjectsTreeImpl: (onResult: () -> Unit) -> Unit
) {
    /** Opens the system folder picker; the callback receives null when cancelled. */
    fun launchDirectory(onResult: (PickedLocation?) -> Unit) =
        launchDirectoryImpl(onResult)

    /** Opens the document picker filtered to plugin archives. */
    fun launchKinetFile(onResult: (Uri?) -> Unit) = launchKinetFileImpl(onResult)

    /**
     * Asks the user to grant persistent access to the default projects folder
     * (`/storage/emulated/0/KometIDEProjects`) or one of its parents.
     */
    fun requestProjectsFolderAccess(onResult: () -> Unit) = launchProjectsTreeImpl(onResult)
}

@Composable
fun rememberSafPickers(): SafPickers {
    val dirCallback = remember { PendingCallback<PickedLocation?>() }
    val fileCallback = remember { PendingCallback<Uri?>() }
    val treeCallback = remember { PendingCallback<Unit>() }

    val dirLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val cb = dirCallback.take() ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            cb(null)
            return@rememberLauncherForActivityResult
        }
        persistPermission(uri)
        cb(
            PickedLocation(
                path = ScopedStorage.displayNameFor(uri),
                uri = uri
            )
        )
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        val cb = fileCallback.take() ?: return@rememberLauncherForActivityResult
        if (uri != null) persistPermission(uri)
        cb(uri)
    }

    val treeLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        val cb = treeCallback.take() ?: return@rememberLauncherForActivityResult
        AndroidContext.appContext?.let { ScopedStorage.saveProjectsTreeUri(it, uri) }
        cb(Unit)
    }

    return remember(dirLauncher, fileLauncher, treeLauncher) {
        SafPickers(
            launchDirectoryImpl = { onResult ->
                dirCallback.set(onResult)
                dirLauncher.launch(null)
            },
            launchKinetFileImpl = { onResult ->
                fileCallback.set(onResult)
                fileLauncher.launch(arrayOf(KINET_MIME, "application/zip"))
            },
            launchProjectsTreeImpl = { onResult ->
                treeCallback.set { onResult() }
                treeLauncher.launch(null)
            }
        )
    }
}

/** MIME type declared for `.kinet`; generic so the system accepts the file everywhere. */
const val KINET_MIME: String = "application/octet-stream"

private fun persistPermission(uri: Uri) {
    val ctx = AndroidContext.appContext ?: return
    runCatching {
        ctx.contentResolver.takePersistableUriPermission(
            uri,
            android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
    }
}

/** Holds the callback for an in-flight picker launch. */
private class PendingCallback<T> {
    private var callback: ((T) -> Unit)? = null

    fun set(cb: (T) -> Unit) {
        callback = cb
    }

    fun take(): ((T) -> Unit)? {
        val cb = callback
        callback = null
        return cb
    }
}
