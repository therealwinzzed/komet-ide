package winzzed.komet.ide.data

object HelpDownloader {
    private const val URL_HELP = "http://plugins.winzzed.online/Komet/Help.md"

    suspend fun load(fallback: String): String =
        httpGetText(URL_HELP, timeoutMs = 5_000) ?: fallback
}
