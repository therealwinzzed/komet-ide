package winzzed.komet.ide.data

/**
 * Minimal ZIP writer that works on every Kotlin target.
 *
 * Entries are stored uncompressed (method 0). A plugin archive holds a handful of
 * small text files, so deflate would save a few hundred bytes at the cost of a
 * platform-specific implementation on each target.
 *
 * The timestamp is fixed at 1980-01-01 so the same project always produces
 * byte-identical archives.
 */
object ZipWriter {

    private const val SIG_LOCAL = 0x04034b50L
    private const val SIG_CENTRAL = 0x02014b50L
    private const val SIG_EOCD = 0x06054b50L

    /** "version needed to extract" 2.0 — required for the UTF-8 name flag. */
    private const val VERSION = 20

    /** Bit 11: file names are UTF-8. */
    private const val FLAG_UTF8 = 0x0800

    private const val METHOD_STORED = 0

    // DOS timestamp for 1980-01-01 00:00:00, the earliest representable value.
    private const val DOS_TIME = 0
    private const val DOS_DATE = 0x0021

    /**
     * Builds an archive from [entries] (name to content).
     * Names must use forward slashes; leading slashes are stripped.
     */
    fun build(entries: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteArrayBuilder()
        val central = ByteArrayBuilder()

        for ((rawName, content) in entries) {
            val name = rawName.trimStart('/').trimEnd('/')
            if (name.isEmpty()) continue

            val nameBytes = name.encodeToByteArray()
            val crc = crc32(content)
            val offset = out.size

            // ---- local file header ----
            out.u32(SIG_LOCAL)
            out.u16(VERSION)
            out.u16(FLAG_UTF8)
            out.u16(METHOD_STORED)
            out.u16(DOS_TIME)
            out.u16(DOS_DATE)
            out.u32(crc)
            out.u32(content.size.toLong())
            out.u32(content.size.toLong())
            out.u16(nameBytes.size)
            out.u16(0) // extra field length
            out.bytes(nameBytes)
            out.bytes(content)

            // ---- matching central directory record ----
            central.u32(SIG_CENTRAL)
            central.u16(VERSION) // version made by
            central.u16(VERSION) // version needed
            central.u16(FLAG_UTF8)
            central.u16(METHOD_STORED)
            central.u16(DOS_TIME)
            central.u16(DOS_DATE)
            central.u32(crc)
            central.u32(content.size.toLong())
            central.u32(content.size.toLong())
            central.u16(nameBytes.size)
            central.u16(0) // extra length
            central.u16(0) // comment length
            central.u16(0) // disk number start
            central.u16(0) // internal attributes
            central.u32(0) // external attributes
            central.u32(offset.toLong())
            central.bytes(nameBytes)
        }

        val centralBytes = central.toByteArray()
        val centralOffset = out.size
        out.bytes(centralBytes)

        // ---- end of central directory ----
        out.u32(SIG_EOCD)
        out.u16(0) // this disk
        out.u16(0) // disk with central directory
        out.u16(entries.size)
        out.u16(entries.size)
        out.u32(centralBytes.size.toLong())
        out.u32(centralOffset.toLong())
        out.u16(0) // comment length

        return out.toByteArray()
    }

    // ---- CRC-32 (IEEE 802.3, reflected polynomial 0xEDB88320) ----

    private val CRC_TABLE = IntArray(256) { index ->
        var c = index
        repeat(8) {
            c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else c ushr 1
        }
        c
    }

    private fun crc32(data: ByteArray): Long {
        var crc = -1
        for (b in data) {
            crc = (crc ushr 8) xor CRC_TABLE[(crc xor b.toInt()) and 0xFF]
        }
        return crc.inv().toLong() and 0xFFFFFFFFL
    }
}

/** Growable little-endian byte sink. */
private class ByteArrayBuilder {
    private var buffer = ByteArray(1024)
    var size: Int = 0
        private set

    private fun ensure(extra: Int) {
        if (size + extra <= buffer.size) return
        var capacity = buffer.size
        while (capacity < size + extra) capacity *= 2
        buffer = buffer.copyOf(capacity)
    }

    fun u16(value: Int) {
        ensure(2)
        buffer[size++] = (value and 0xFF).toByte()
        buffer[size++] = ((value ushr 8) and 0xFF).toByte()
    }

    fun u32(value: Long) {
        ensure(4)
        buffer[size++] = (value and 0xFF).toByte()
        buffer[size++] = ((value ushr 8) and 0xFF).toByte()
        buffer[size++] = ((value ushr 16) and 0xFF).toByte()
        buffer[size++] = ((value ushr 24) and 0xFF).toByte()
    }

    fun bytes(data: ByteArray) {
        ensure(data.size)
        data.copyInto(buffer, size)
        size += data.size
    }

    fun toByteArray(): ByteArray = buffer.copyOf(size)
}
