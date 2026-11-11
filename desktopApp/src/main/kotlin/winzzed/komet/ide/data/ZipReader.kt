package winzzed.komet.ide.data

/**
 * Minimal ZIP reader that works on every Kotlin target.
 *
 * Counterpart to [ZipWriter]. Plugin archives produced here store entries
 * uncompressed, but archives built by third-party tools may use deflate, so both
 * methods are supported. Data descriptors (flag bit 3) are tolerated because an
 * archive from a streaming zip always sets them.
 */
object ZipReader {

    private const val SIG_LOCAL = 0x04034b50L
    private const val SIG_CENTRAL = 0x02014b50L
    private const val SIG_EOCD = 0x06054b50L

    private const val METHOD_STORED = 0
    private const val METHOD_DEFLATED = 8

    /**
     * Extracts every file entry. Directory entries are skipped.
     *
     * Returns null when [bytes] is not a readable ZIP archive, and throws
     * [UnsupportedOperationException] when an entry uses deflate but no inflater
     * was supplied for this platform.
     */
    fun extract(
        bytes: ByteArray,
        inflater: ((ByteArray) -> ByteArray)? = null
    ): List<Pair<String, ByteArray>>? {
        val eocd = findEocd(bytes) ?: return null

        val entryCount = bytes.u16(eocd + 10)
        val centralOffset = bytes.u32(eocd + 16)

        if (centralOffset <= 0 || centralOffset >= bytes.size) return null

        val result = mutableListOf<Pair<String, ByteArray>>()
        var cursor = centralOffset.toInt()

        repeat(entryCount) {
            if (cursor + 46 > bytes.size) return@repeat
            if (bytes.u32(cursor) != SIG_CENTRAL) return@repeat

            val method = bytes.u16(cursor + 10)
            val compressedSize = bytes.u32(cursor + 20).toInt()
            val nameLen = bytes.u16(cursor + 28)
            val extraLen = bytes.u16(cursor + 30)
            val commentLen = bytes.u16(cursor + 32)
            val localOffset = bytes.u32(cursor + 42).toInt()

            val nameStart = cursor + 46
            if (nameStart + nameLen > bytes.size) return@repeat
            val name = bytes.decodeToString(nameStart, nameStart + nameLen)

            cursor = nameStart + nameLen + extraLen + commentLen

            // Directory entries end with a slash and carry no content.
            if (name.endsWith("/")) return@repeat
            if (localOffset < 0 || localOffset + 30 > bytes.size) return@repeat
            if (bytes.u32(localOffset) != SIG_LOCAL) return@repeat

            // The local header repeats the name/extra lengths, which may differ from
            // the central record, so the data offset must be computed from local.
            val localNameLen = bytes.u16(localOffset + 26)
            val localExtraLen = bytes.u16(localOffset + 28)
            val dataStart = localOffset + 30 + localNameLen + localExtraLen

            if (dataStart + compressedSize > bytes.size) return@repeat
            val raw = bytes.copyOfRange(dataStart, dataStart + compressedSize)

            val content = when (method) {
                METHOD_STORED -> raw
                METHOD_DEFLATED -> inflater?.invoke(raw)
                    ?: throw UnsupportedOperationException(
                        "Deflate is not supported on this platform"
                    )

                else -> return@repeat
            }

            result += name to content
        }

        return result
    }

    /** Locates the end-of-central-directory record, scanning backwards over a comment. */
    private fun findEocd(bytes: ByteArray): Int? {
        // The EOCD is 22 bytes plus an optional comment of at most 65535 bytes.
        val minStart = (bytes.size - 22 - 0xFFFF).coerceAtLeast(0)
        var i = bytes.size - 22
        while (i >= minStart) {
            if (bytes.u32(i) == SIG_EOCD) return i
            i--
        }
        return null
    }

    private fun ByteArray.u16(at: Int): Int =
        (this[at].toInt() and 0xFF) or ((this[at + 1].toInt() and 0xFF) shl 8)

    private fun ByteArray.u32(at: Int): Long =
        (this[at].toLong() and 0xFF) or
            ((this[at + 1].toLong() and 0xFF) shl 8) or
            ((this[at + 2].toLong() and 0xFF) shl 16) or
            ((this[at + 3].toLong() and 0xFF) shl 24)
}
