package winzzed.komet.ide.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import winzzed.komet.ide.data.HelpDownloader
import winzzed.komet.ide.data.ProjectFile
import winzzed.komet.ide.data.ProjectStorage
import winzzed.komet.ide.platform.DownloadResult
import winzzed.komet.ide.platform.saveToDownloads

/**
 * Drives the IDE screen. Uses only common APIs so it compiles for every target.
 *
 * [helpFallback] is the platform-provided default help text (desktop loads it from
 * resources, other targets pass a short inline string).
 */
class IdeViewModel(
    basePath: String,
    private val helpFallback: String = bundledHelpText()
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var repo = ProjectStorage(basePath)

    private val _files = MutableStateFlow<List<ProjectFile>>(emptyList())
    val files: StateFlow<List<ProjectFile>> = _files.asStateFlow()

    private val _selected = MutableStateFlow<ProjectFile?>(null)
    val selected: StateFlow<ProjectFile?> = _selected.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private var saveJob: Job? = null

    init { load(basePath) }

    fun reload(newBasePath: String) {
        repo = ProjectStorage(newBasePath)
        _files.value = emptyList()
        _selected.value = null
        _content.value = ""
        load(newBasePath)
    }

    private fun load(path: String) {
        scope.launch {
            try {
                repo.ensureSkeleton()
                val text = HelpDownloader.load(helpFallback)
                repo.writeHelpIfMissing(text)
                refresh()
                select(_files.value.firstOrNull())
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    fun refresh() { _files.value = repo.listFiles() }

    fun select(file: ProjectFile?) {
        _selected.value = file
        _content.value = file?.let { repo.read(it) } ?: ""
    }

    fun onEdit(newText: String) {
        _content.value = newText
        val f = _selected.value ?: return
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            repo.write(f, newText)
        }
    }

    fun createFile(name: String) {
        repo.create(name)
        refresh()
    }

    /**
     * Compiles the project into plugin.kinet and saves it to the platform's
     * Downloads folder. Returns the outcome so the caller can report the location.
     */
    suspend fun compilePlugin(): DownloadResult {
        val bytes = repo.buildKinetZipBytes()
            ?: return DownloadResult(false, message = "Не удалось собрать архив")

        return saveToDownloads(PLUGIN_FILE_NAME, bytes)
    }

    fun close() { scope.cancel() }

    companion object {
        const val DEFAULT_HELP: String = "# Help"
        const val PLUGIN_FILE_NAME: String = "plugin.kinet"
    }
}
