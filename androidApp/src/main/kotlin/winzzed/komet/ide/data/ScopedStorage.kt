package winzzed.komet.ide.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import java.io.File

/**
 * Scoped-storage (SAF) plumbing.
 *
 * Android has no API that returns a writable absolute path from a folder picker.
 * The system picker returns a tree URI instead, and every read/write must go
 * through [ContentResolver]. This object keeps the granted tree and exposes the
 * few operations the project file browser needs.
 */
object ScopedStorage {

    private const val PREFS = "komet_ide_storage"
    private const val KEY_PROJECTS_TREE = "projects_tree_uri"

    // ---- Persisted grants ---------------------------------------------------

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun saveProjectsTreeUri(context: Context, uri: Uri?) {
        val editor = prefs(context).edit()
        if (uri == null) editor.remove(KEY_PROJECTS_TREE) else editor.putString(
            KEY_PROJECTS_TREE,
            uri.toString()
        )
        editor.apply()

        if (uri != null) {
            // Without this the grant dies with the process and the folder is
            // unreadable after the next launch.
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
    }

    fun projectsTreeUri(): Uri? {
        val ctx = AndroidContext.appContext ?: return null
        val raw = prefs(ctx).getString(KEY_PROJECTS_TREE, null) ?: return null
        return runCatching { Uri.parse(raw) }.getOrNull()
    }

    /**
     * Locates the URI of the real `KometIDEProjects` folder inside the granted tree.
     *
     * The picker may hand back the parent (`/storage/emulated/0`) if the user chose a
     * higher level, so the child folder is resolved and, when missing, created.
     */
    fun projectsFolderUri(treeUri: Uri): Uri? {
        val ctx = AndroidContext.appContext ?: return null
        val resolver = ctx.contentResolver

        val treeDocId = runCatching {
            DocumentsContract.getTreeDocumentId(treeUri)
        }.getOrNull() ?: return null

        // The tree may already point at the projects folder.
        if (treeDocId.endsWith(":KometIDEProjects")) {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        }

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            treeDocId
        )

        findChild(resolver, childrenUri, "KometIDEProjects")?.let { return it }

        // Create it inside the granted tree so the default folder really exists.
        return runCatching {
            DocumentsContract.createDocument(
                resolver,
                DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId),
                DocumentsContract.Document.MIME_TYPE_DIR,
                "KometIDEProjects"
            )
        }.getOrNull()
    }

    private fun findChild(
        resolver: ContentResolver,
        childrenUri: Uri,
        name: String
    ): Uri? {
        return runCatching {
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    if (cursor.getString(1) == name) {
                        return@use DocumentsContract.buildDocumentUriUsingTree(
                            childrenUri,
                            cursor.getString(0)
                        )
                    }
                }
                null
            }
        }.getOrNull()
    }

    fun isWritable(uri: Uri): Boolean {
        val ctx = AndroidContext.appContext ?: return false
        return runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { true } ?: false
        }.getOrDefault(false)
    }

    // ---- Directory browsing -------------------------------------------------

    /** One entry in a SAF folder listing. */
    data class Entry(
        val name: String,
        val uri: Uri,
        val isDirectory: Boolean
    )

    fun listChildren(folderUri: Uri): List<Entry> {
        val ctx = AndroidContext.appContext ?: return emptyList()
        val resolver = ctx.contentResolver

        val docId = runCatching {
            DocumentsContract.getDocumentId(folderUri)
        }.getOrNull() ?: return emptyList()

        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            folderUri,
            docId
        )

        return runCatching {
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val id = cursor.getString(0)
                        val name = cursor.getString(1) ?: continue
                        val mime = cursor.getString(2)
                        add(
                            Entry(
                                name = name,
                                uri = DocumentsContract.buildDocumentUriUsingTree(
                                    folderUri,
                                    id
                                ),
                                isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                            )
                        )
                    }
                }
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }

    /**
     * Reads the text of a SAF document. Falls back to [fallback] when the document
     * cannot be read, so callers can treat "" and a failure the same way.
     */
    fun readText(uri: Uri): String? {
        val ctx = AndroidContext.appContext ?: return null
        return runCatching {
            ctx.contentResolver.openInputStream(uri)?.use {
                it.readBytes().decodeToString()
            }
        }.getOrNull()
    }

    fun readBytes(uri: Uri): ByteArray? {
        val ctx = AndroidContext.appContext ?: return null
        return runCatching {
            ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }.getOrNull()
    }

    /**
     * Writes text into a SAF document, replacing its content.
     *
     * SAF has no random-access write through this API, so the whole buffer is
     * written at once. Returns false when the grant no longer covers the document.
     */
    fun writeText(uri: Uri, content: String): Boolean {
        val ctx = AndroidContext.appContext ?: return false
        return runCatching {
            ctx.contentResolver.openOutputStream(uri, "wt")?.use {
                it.write(content.encodeToByteArray())
                true
            } ?: false
        }.getOrDefault(false)
    }

    /** Creates a file inside a SAF directory and returns its URI. */
    fun createFile(parentUri: Uri, name: String, mimeType: String): Uri? {
        val ctx = AndroidContext.appContext ?: return null
        val docId = runCatching {
            DocumentsContract.getDocumentId(parentUri)
        }.getOrNull() ?: return null

        return runCatching {
            DocumentsContract.createDocument(
                ctx.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(parentUri, docId),
                mimeType,
                name
            )
        }.getOrNull()
    }

    /** A short display path for a SAF tree, e.g. `KometIDEProjects/journal`. */
    fun displayNameFor(uri: Uri): String {
        val ctx = AndroidContext.appContext ?: return uri.toString()
        val docId = runCatching {
            DocumentsContract.getDocumentId(uri)
        }.getOrNull() ?: return uri.toString()

        // Tree ids look like "primary:Folder/Sub"; show the part after the colon.
        val tail = docId.substringAfter(':', docId)
        return tail.ifBlank {
            runCatching {
                DocumentsContract.getTreeDocumentId(uri).substringAfter(':', "Хранилище")
            }.getOrDefault("Хранилище")
        }
    }
}

/**
 * Result of a SAF pick. [path] is the human-readable location shown in the UI;
 * [uri] is what every subsequent I/O must use.
 */
data class PickedLocation(
    val path: String,
    val uri: Uri?
)

