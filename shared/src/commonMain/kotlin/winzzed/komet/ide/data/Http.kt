package winzzed.komet.ide.data

/** Minimal cross-platform HTTP GET returning the body, or null on any failure. */
expect suspend fun httpGetText(url: String, timeoutMs: Int = 5_000): String?

/** True when the platform can reach the network and open sockets. */
expect val platformSupportsNetwork: Boolean
