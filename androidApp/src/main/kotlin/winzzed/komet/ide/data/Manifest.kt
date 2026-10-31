package winzzed.komet.ide.data

/**
 * Reading and editing of a plugin's manifest.json.
 *
 * The editor intentionally does targeted regex replacements instead of a full
 * parse/serialize round-trip so comments, key order and unknown fields survive.
 */

/** A single `"key": "value"` pair shown as an editable field. */
data class ManifestField(
    val key: String,
    val originalValue: String,
    var editValue: String
)

/** Permissions offered as check-boxes in the settings screen. */
val ALL_PERMISSIONS: List<String> = listOf(
    "chat.write",
    "chat.edit",
    "chat.photo",
    "chat.file",
    "ui.notify",
    "contact.read",
    "message.readReply",
    "network",
    "storage"
)

/** Keys handled by their own UI section rather than as plain text fields. */
private val STRUCTURED_KEYS = setOf("permissions", "commands")

/** Extracts the flat `"key": "value"` pairs, skipping structured keys. */
fun parseManifestFields(raw: String): List<ManifestField> {
    val fields = mutableListOf<ManifestField>()
    val flatRegex = """"([^"]+)"\s*:\s*"([^"]*)"""".toRegex()
    flatRegex.findAll(raw).forEach { m ->
        val key = m.groupValues[1]
        if (key in STRUCTURED_KEYS) return@forEach
        fields += ManifestField(
            key = key,
            originalValue = m.groupValues[2],
            editValue = m.groupValues[2]
        )
    }
    return fields
}

/** Reads the `permissions` array; empty when absent. */
fun parsePermissions(raw: String): Set<String> {
    val regex = """"permissions"\s*:\s*\[([^\]]*)]""".toRegex()
    val match = regex.find(raw) ?: return emptySet()
    val itemRegex = """"([^"]+)"""".toRegex()
    return itemRegex.findAll(match.groupValues[1]).map { it.groupValues[1] }.toSet()
}

/**
 * Writes [permissions] back into the manifest, creating the array when the
 * manifest does not have one yet.
 */
fun replacePermissions(raw: String, permissions: Set<String>): String {
    val newArray = permissions.sorted().joinToString(", ") { "\"$it\"" }
    val regex = """"permissions"\s*:\s*\[([^\]]*)]""".toRegex()

    if (regex.containsMatchIn(raw)) {
        return regex.replace(raw) { """"permissions": [$newArray]""" }
    }

    val closing = raw.lastIndexOf('}')
    if (closing < 0) return raw

    val head = raw.substring(0, closing).trimEnd()
    val separator = if (head.endsWith("{")) "" else ","
    return "$head$separator\n  \"permissions\": [$newArray]\n}\n"
}

/** Applies edited field values, leaving untouched keys exactly as they were. */
fun applyEditsToRaw(raw: String, fields: List<ManifestField>): String {
    var result = raw
    for (field in fields) {
        if (field.editValue == field.originalValue) continue

        val escapedNew = field.editValue
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")

        val pattern = """("${Regex.escape(field.key)}"\s*:\s*")([^"]*)(")""".toRegex()
        // Splice manually: the String form of replaceFirst() interprets `\` and `$`
        // in the replacement, which would eat the escaping and emit invalid JSON.
        val match = pattern.find(result) ?: continue
        result = buildString {
            append(result, 0, match.range.first)
            append(match.groupValues[1])
            append(escapedNew)
            append(match.groupValues[3])
            append(result, match.range.last + 1, result.length)
        }
    }
    return result
}
