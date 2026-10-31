package winzzed.komet.ide.data

/**
 * Base64 encoding for handing binary data to the browser.
 *
 * The web targets pass a `data:` URL to the browser's download machinery. Crossing
 * the JS boundary with a plain String keeps that working identically under Kotlin/JS
 * and Kotlin/Wasm, where a ByteArray would need Uint8Array interop.
 */
private const val ALPHABET =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

fun ByteArray.encodeBase64(): String {
    if (isEmpty()) return ""

    val out = StringBuilder((size + 2) / 3 * 4)
    var i = 0
    while (i < size) {
        val b0 = this[i].toInt() and 0xFF
        val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else -1
        val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else -1

        out.append(ALPHABET[b0 shr 2])
        out.append(
            when {
                b1 < 0 -> ALPHABET[(b0 and 0x03) shl 4]
                else -> ALPHABET[((b0 and 0x03) shl 4) or (b1 shr 4)]
            }
        )
        out.append(
            when {
                b1 < 0 -> '='
                b2 < 0 -> ALPHABET[(b1 and 0x0F) shl 2]
                else -> ALPHABET[((b1 and 0x0F) shl 2) or (b2 shr 6)]
            }
        )
        out.append(
            when {
                b2 < 0 -> '='
                else -> ALPHABET[b2 and 0x3F]
            }
        )
        i += 3
    }
    return out.toString()
}

/** Inverse of [encodeBase64]; used to verify the encoder in tests. */
fun String.decodeBase64(): ByteArray {
    val cleaned = filterNot { it.isWhitespace() }
    if (cleaned.isEmpty()) return ByteArray(0)

    val out = ArrayList<Byte>(cleaned.length / 4 * 3)
    var i = 0
    while (i < cleaned.length) {
        val c0 = decodeChar(cleaned[i])
        val c1 = if (i + 1 < cleaned.length) decodeChar(cleaned[i + 1]) else -1
        val c2 = if (i + 2 < cleaned.length) decodeChar(cleaned[i + 2]) else -1
        val c3 = if (i + 3 < cleaned.length) decodeChar(cleaned[i + 3]) else -1

        if (c0 < 0 || c1 < 0) break
        out.add((((c0 shl 2) or (c1 shr 4)) and 0xFF).toByte())
        if (c2 >= 0) {
            out.add((((c1 and 0x0F) shl 4) or (c2 shr 2) and 0xFF).toByte())
            if (c3 >= 0) {
                out.add((((c2 and 0x03) shl 6) or c3).toByte())
            }
        }
        i += 4
    }
    return out.toByteArray()
}

private fun decodeChar(c: Char): Int = when (c) {
    in 'A'..'Z' -> c - 'A'
    in 'a'..'z' -> c - 'a' + 26
    in '0'..'9' -> c - '0' + 52
    '+' -> 62
    '/' -> 63
    else -> -1 // '=' padding or an unknown character
}
