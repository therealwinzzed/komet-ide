package winzzed.komet.ide.data

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Round-trips a project through the `.kinet` archive format: build it the way the
 * IDE does, then import it back and confirm the folder is editable again.
 */
class KinetImportTest {

    private fun tempDir(prefix: String): File =
        File.createTempFile(prefix, "").let { file ->
            file.delete()
            file.mkdirs()
            file
        }

    @Test
    fun `archive built from a project imports back with the same files`() {
        val root = tempDir("kinet-src")
        val source = File(root, "My Application").apply { mkdirs() }
        File(source, "manifest.json").writeText(
            """
            {
              "name": "Komet Info",
              "package": "winzzed.komet.info",
              "version": "1.0.0"
            }
            """.trimIndent()
        )
        File(source, "main.js").writeText("// main.js\nconsole.log('hi')\n")

        val bytes = ProjectStorage(source.absolutePath).buildKinetZipBytes()
        assertNotNull(bytes, "archive should build")
        assertTrue(bytes.isNotEmpty())

        val target = tempDir("kinet-dst")
        val result = importKinetProject(bytes, "test-plugin.kinet", target.absolutePath)

        assertTrue(result.ok, "import failed: ${result.message}")
        assertEquals(2, result.fileCount)

        // The folder is named after the manifest, not the archive file name.
        val project = File(result.projectPath!!)
        assertEquals("Komet Info", project.name)

        val manifest = File(project, "manifest.json").readText()
        assertTrue(manifest.contains("\"name\": \"Komet Info\""))
        assertTrue(File(project, "main.js").readText().contains("console.log"))
    }

    @Test
    fun `imported project can be edited and rebuilt`() {
        val root = tempDir("kinet-edit")
        val source = File(root, "Original").apply { mkdirs() }
        File(source, "manifest.json").writeText("""{"name":"Edit Me","version":"1.0.0"}""")
        File(source, "main.js").writeText("// v1\n")

        val archive = ProjectStorage(source.absolutePath).buildKinetZipBytes()!!
        val target = tempDir("kinet-edit-dst")
        val imported = importKinetProject(archive, "edit.kinet", target.absolutePath)
        assertTrue(imported.ok, imported.message)

        // Modify the imported copy and compile it again.
        val project = imported.projectPath!!
        File(project, "main.js").writeText("// v2 changed\n")
        val rebuilt = ProjectStorage(project).buildKinetZipBytes()

        assertNotNull(rebuilt)
        val entries = ZipReader.extract(rebuilt)
        assertNotNull(entries)
        val mainJs = entries.first { it.first == "main.js" }.second.decodeToString()
        assertTrue(mainJs.contains("v2 changed"), "rebuild should contain the edit")
    }

    @Test
    fun `import rejects a file that is not an archive`() {
        val target = tempDir("kinet-bad")
        val result = importKinetProject("not a zip".encodeToByteArray(), "bad.kinet", target.absolutePath)
        assertTrue(!result.ok)
        assertNotNull(result.message)
    }

    @Test
    fun `deflated entries from a foreign zip are read`() {
        // A zip written by another tool uses deflate; import must still handle it.
        val root = tempDir("kinet-deflate")
        val zipFile = File(root, "foreign.kinet")
        java.util.zip.ZipOutputStream(zipFile.outputStream()).use { out ->
            out.setMethod(java.util.zip.ZipOutputStream.DEFLATED)
            out.putNextEntry(java.util.zip.ZipEntry("manifest.json"))
            out.write("""{"name":"Deflated","version":"2.0.0"}""".encodeToByteArray())
            out.closeEntry()
        }

        // Diagnose the archive structure before importing.
        val rawZip = zipFile.readBytes()
        val direct = ZipReader.extract(rawZip) { raw -> inflate(raw) }
        assertNotNull(direct, "ZipReader.extract returned null for a deflated zip")
        assertEquals(1, direct.size)
        assertTrue(direct[0].second.decodeToString().contains("Deflated"))
        val target = tempDir("kinet-deflate-dst")
        val result = importKinetProject(zipFile.readBytes(), "foreign.kinet", target.absolutePath)
        assertTrue(result.ok, result.message)
        val manifest = File(result.projectPath!!, "manifest.json").readText()
        assertTrue(manifest.contains("Deflated"))
    }

    @Test
    fun `archive entries cannot escape the project folder`() {
        val root = tempDir("kinet-escape")
        val evil = ZipWriter.build(
            listOf(
                "../escaped.txt" to "pwned".encodeToByteArray(),
                "manifest.json" to """{"name":"Safe"}""".encodeToByteArray()
            )
        )

        val target = tempDir("kinet-escape-dst")
        val result = importKinetProject(evil, "evil.kinet", target.absolutePath)

        assertTrue(result.ok, result.message)
        // Only the legitimate entry should have been written.
        assertEquals(1, result.fileCount)
        assertTrue(!File(target.parentFile, "escaped.txt").exists())
    }
}
