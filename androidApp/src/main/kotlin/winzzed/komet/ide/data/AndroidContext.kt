package winzzed.komet.ide.data

import android.content.Context

/**
 * Holds the application context for the few Android APIs that need it
 * (SharedPreferences, files dir). Call [init] from Application.onCreate or
 * MainActivity before using storage.
 */
object AndroidContext {
    @Volatile
    var appContext: Context? = null
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
    }
}
