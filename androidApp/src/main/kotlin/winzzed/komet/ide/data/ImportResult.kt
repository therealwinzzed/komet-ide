package winzzed.komet.ide.data

/** Outcome of importing a `.kinet` archive into a project folder. */
data class ImportResult(
    val ok: Boolean,
    val projectPath: String? = null,
    val fileCount: Int = 0,
    val message: String? = null
)
