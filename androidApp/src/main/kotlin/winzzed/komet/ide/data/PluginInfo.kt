package winzzed.komet.ide.data

/**
 * Read-only view of a plugin's manifest used by the plugin tester window.
 *
 * Unlike the settings editor this does not need to preserve formatting: it only
 * extracts the values shown in the "plugin info" header.
 */
data class PluginInfo(
    val name: String,
    val packageName: String,
    val version: String,
    val description: String,
    val commands: List<String>
)

/**
 * Parses the values the tester displays, tolerating a manifest that is still being
 * edited: every field falls back to an empty string instead of throwing.
 */
fun parsePluginInfo(raw: String): PluginInfo {
    val name = extractJsonString(raw, "name").orEmpty()
    val packageName = extractJsonString(raw, "package").orEmpty()
    val version = extractJsonString(raw, "version").orEmpty()
    val description = extractJsonString(raw, "description").orEmpty()

    return PluginInfo(
        name = name.ifBlank { packageName.ifBlank { "Плагин" } },
        packageName = packageName,
        version = version.ifBlank { "—" },
        description = description,
        commands = parseCommands(raw)
    )
}

/**
 * Reads the `commands` array. Each entry may be a plain string ("ping") or an
 * object with a `name`/`command` field, which is what real manifests use.
 */
fun parseCommands(raw: String): List<String> {
    val arrayRegex = """"commands"\s*:\s*\[(.*?)]""".toRegex(RegexOption.DOT_MATCHES_ALL)
    val body = arrayRegex.find(raw)?.groupValues?.get(1) ?: return emptyList()

    val asStrings = """"([^"]+)"""".toRegex()
        .findAll(body)
        .map { it.groupValues[1] }
        .toList()

    // An array of objects yields both the key names and the values; keep only the
    // values of name/command/id so "description" text is not mistaken for a command.
    val keyed = """("(?:name|command|id)"\s*:\s*")([^"]*)"""".toRegex()
        .findAll(body)
        .map { it.groupValues[2] }
        .toList()

    return (if (keyed.isNotEmpty()) keyed else asStrings)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
}

/** Minimal JSON string extraction shared by the update checker and the tester. */
private fun extractJsonString(json: String, key: String): String? {
    val regex = ("\"" + Regex.escape(key) + "\"\\s*:\\s*\"([^\"]*)\"").toRegex()
    return regex.find(json)?.groupValues?.get(1)
}
