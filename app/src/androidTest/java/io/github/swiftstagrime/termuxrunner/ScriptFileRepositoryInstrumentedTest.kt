package io.github.swiftstagrime.termuxrunner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptFileRepositoryImpl
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ScriptFileRepositoryInstrumentedTest {
    private lateinit var repository: ScriptFileRepositoryImpl
    private lateinit var bridgeDir: File
    private val bridgeFolderName = "TermuxRunnerBridge"

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        repository = ScriptFileRepositoryImpl(context)

        bridgeDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS),
                bridgeFolderName,
            )
        } else {
            File(
                android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS,
                ),
                bridgeFolderName,
            )
        }

        if (bridgeDir.exists()) {
            deleteBridgeContents()
        }
    }

    @After
    fun cleanup() {
        deleteBridgeContents()
    }

    private fun deleteBridgeContents() {
        if (bridgeDir.isDirectory) {
            bridgeDir.listFiles()?.forEach { it.delete() }
        }
    }

    @Test
    fun saveToBridge_createsDirectoryIfMissing() {
        if (bridgeDir.exists()) {
            bridgeDir.deleteRecursively()
        }
        assertFalse(bridgeDir.exists())

        val path = repository.saveToBridge("test.sh", "echo hello")

        assertTrue(bridgeDir.exists())
        assertTrue(bridgeDir.isDirectory)
    }

    @Test
    fun saveToBridge_writesCorrectContent() {
        val code = "#!/bin/bash\necho 'Hello, World!'\nexit 0"
        val path = repository.saveToBridge("hello.sh", code)

        val file = File(path)
        assertTrue(file.exists())
        assertEquals(code, file.readText())
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

        val file = File(bridgeDir, fileName)
        assertEquals("echo old", file.readText())

        repository.saveToBridge(fileName, "echo new")
        assertEquals("echo new", file.readText())
    }

    @Test
    fun saveToBridge_largeScript_integrityPreserved() {
        val largeCode = List(10000) { i -> "line_$i: echo step $i" }.joinToString("\n")
        val expectedSize = largeCode.toByteArray().size

        val path = repository.saveToBridge("large.sh", largeCode)
        val file = File(path)

        assertTrue(file.exists())
        assertEquals(expectedSize, file.length().toInt())
        assertEquals(largeCode, file.readText())
    }

    @Test
    fun saveToBridge_specialCharactersInFilename() {
        val code = "echo test"
        val path = repository.saveToBridge("my script (v2).sh", code)

        val file = File(path)
        assertTrue(file.exists())
        assertEquals(code, file.readText())
    }

    @Test
    fun saveToBridge_emptyScript() {
        val path = repository.saveToBridge("empty.sh", "")

        val file = File(path)
        assertTrue(file.exists())
        assertEquals("", file.readText())
    }

    @Test
    fun saveToBridge_unicodeContent() {
        val code = "#!/bin/bash\necho 'Привет мир'\necho 'こんにちは'"
        val path = repository.saveToBridge("unicode.sh", code)

        val file = File(path)
        assertEquals(code, file.readText())
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
            assertEquals("echo ${index + 1}", File(path).readText())
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
