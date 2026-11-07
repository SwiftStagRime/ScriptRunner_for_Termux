package io.github.swiftstagrime.termuxrunner

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptFileRepositoryImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

// Bridge files live in public Downloads: pre-Q via the File API, Q+ via
// MediaStore (issue #25); on Q+ the owner reads/deletes them via resolver.
@RunWith(AndroidJUnit4::class)
class ScriptFileRepositoryInstrumentedTest {
    private lateinit var repository: ScriptFileRepositoryImpl
    private lateinit var context: Context
    private lateinit var bridgeDir: File
    private val bridgeFolderName = "TermuxRunnerBridge"
    private val bridgeRelativePath = "${Environment.DIRECTORY_DOWNLOADS}/$bridgeFolderName"

    private val usesMediaStore: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = ScriptFileRepositoryImpl(context)
        bridgeDir =
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                bridgeFolderName,
            )
        deleteBridgeContents()
    }

    @After
    fun cleanup() {
        deleteBridgeContents()
    }

    private fun deleteBridgeContents() {
        if (usesMediaStore) {
            context.contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("$bridgeRelativePath%"),
            )
            // Rows whose owner was never set are invisible to the app's own
            // scoped deletes; the instrumentation shell UID can remove them.
            runCatching {
                val command =
                    arrayOf(
                        "content",
                        "delete",
                        "--uri",
                        "content://media/external/downloads",
                        "--where",
                        "relative_path LIKE '$bridgeRelativePath%'",
                    )
                val process = Runtime.getRuntime().exec(command)
                process.waitFor()
            }
        } else {
            bridgeDir.listFiles()?.forEach { it.delete() }
        }
    }

    private fun readBridgeFile(
        fileName: String,
        relativePath: String = bridgeRelativePath,
    ): String? {
        if (!usesMediaStore) {
            return File(bridgeDir, fileName).readText()
        }
        val resolver = context.contentResolver
        val cursor =
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                    "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf("$relativePath/", fileName),
                null,
            )
        cursor?.use {
            if (!it.moveToFirst()) return null
            val uri =
                ContentUris.withAppendedId(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    it.getLong(0),
                )
            return resolver.openInputStream(uri)?.readBytes()?.decodeToString()
        }
        return null
    }

    @Test
    fun saveToBridge_createsDirectoryIfMissing() {
        if (!usesMediaStore) {
            bridgeDir.deleteRecursively()
            assertFalse(bridgeDir.exists())
        }

        val path = repository.saveToBridge("missing_dir.sh", "echo hello")

        assertTrue(bridgeDir.exists())
        assertTrue(bridgeDir.isDirectory)
        assertEquals("echo hello", readBridgeFile("missing_dir.sh"))
        assertNotNull(path)
    }

    @Test
    fun saveToBridge_writesCorrectContent() {
        val code = "#!/bin/bash\necho 'Hello, World!'\nexit 0"
        val path = repository.saveToBridge("hello.sh", code)

        assertTrue(path.startsWith(bridgeDir.absolutePath))
        assertEquals(code, readBridgeFile("hello.sh"))
    }

    @Test
    fun saveToBridge_returnsPathUnderBridgeDirectory() {
        val path = repository.saveToBridge("script.sh", "ls")

        assertTrue("Path should be under bridge directory", path.startsWith(bridgeDir.absolutePath))
        assertTrue(path.endsWith("/script.sh"))
    }

    @Test
    fun saveToBridge_overwritesExistingFile() {
        val fileName = "overwrite.sh"
        repository.saveToBridge(fileName, "echo old")
        assertEquals("echo old", readBridgeFile(fileName))

        repository.saveToBridge(fileName, "echo new")
        assertEquals("echo new", readBridgeFile(fileName))
    }

    @Test
    fun saveToBridge_largeScript_integrityPreserved() {
        val largeCode = List(10000) { i -> "line_$i: echo step $i" }.joinToString("\n")

        repository.saveToBridge("large.sh", largeCode)

        assertEquals(largeCode, readBridgeFile("large.sh"))
    }

    @Test
    fun saveToBridge_specialCharactersInFilename() {
        val fileName = "my script (v2).sh"
        repository.saveToBridge(fileName, "echo test")

        assertEquals("echo test", readBridgeFile(fileName))
    }

    @Test
    fun saveToBridge_emptyScript() {
        repository.saveToBridge("empty.sh", "")

        assertEquals("", readBridgeFile("empty.sh"))
    }

    @Test
    fun saveToBridge_unicodeContent() {
        val code = "#!/bin/bash\necho 'Привет мир'\necho 'こんにちは'"
        repository.saveToBridge("unicode.sh", code)

        assertEquals(code, readBridgeFile("unicode.sh"))
    }

    @Test
    fun saveToBridge_multipleFilesCoexist() {
        val paths = mutableListOf<String>()

        for (i in 1..5) {
            val path = repository.saveToBridge("script_$i.sh", "echo $i")
            paths.add(path)
        }

        assertEquals(5, paths.size)
        assertTrue(paths.toSet().size == 5)

        paths.forEachIndexed { index, path ->
            assertTrue(path.startsWith(bridgeDir.absolutePath))
            assertEquals("echo ${index + 1}", readBridgeFile("script_${index + 1}.sh"))
        }
    }

    @Test
    fun saveToBridge_fileExtensionPreserved() {
        val extensions = listOf(".sh", ".py", ".js")
        extensions.forEach { ext ->
            val path = repository.saveToBridge("test$ext", "code")
            assertTrue("Path should end with $ext", path.endsWith(ext))
        }
    }
}
