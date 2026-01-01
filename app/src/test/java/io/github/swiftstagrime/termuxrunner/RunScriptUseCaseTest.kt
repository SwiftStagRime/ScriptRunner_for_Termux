package io.github.swiftstagrime.termuxrunner

import android.util.Base64
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.mockk.MockKAnnotations
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class RunScriptUseCaseTest {

    @MockK
    lateinit var termuxRepository: TermuxRepository

    @MockK
    lateinit var scriptFileRepository: ScriptFileRepository

    @MockK
    lateinit var monitoringRepository: MonitoringRepository

    private lateinit var useCase: RunScriptUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(Base64::class)

        every {
            Base64.encodeToString(any<ByteArray>(), any())
        } answers { call ->
            val bytes = call.invocation.args[0] as ByteArray
            java.util.Base64.getEncoder().encodeToString(bytes)
        }

        useCase = RunScriptUseCase(termuxRepository, scriptFileRepository, monitoringRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    @Test
    fun `invoke with LARGE script (4000 chars) uses file bridge strategy`() = runTest {
        val largeCode = "a".repeat(4005)

        val script = Script(
            id = 99,
            name = "Large Script",
            code = largeCode,
            interpreter = "python",
            fileExtension = "py",
            commandPrefix = "",
            runInBackground = true,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = emptyMap(),
            keepSessionOpen = false
        )

        val fakeSavedPath = "/sdcard/Download/TermuxRunnerBridge/script_fake.py"

        every {
            scriptFileRepository.saveToBridge(any(), largeCode)
        } returns fakeSavedPath

        val commandSlot = slot<String>()
        every { termuxRepository.runCommand(capture(commandSlot), any(), any()) } returns Unit

        useCase(script)

        val generatedCommand = commandSlot.captured

        verify { scriptFileRepository.saveToBridge(any(), largeCode) }

        assertTrue(
            "Command should copy from bridge path",
            generatedCommand.contains("cp -f $fakeSavedPath")
        )

        assertTrue(
            "Large scripts should not be base64 encoded into the command line",
            !generatedCommand.contains("base64 -d")
        )
    }

    @Test
    fun `invoke with SMALL script uses base64 strategy`() = runTest {
        val smallCode = "print('hello')"
        val script = Script(
            id = 1,
            name = "Small",
            code = smallCode,
            interpreter = "python",
            fileExtension = "py",
            commandPrefix = "",
            runInBackground = true,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = emptyMap(),
            keepSessionOpen = false
        )

        every { termuxRepository.runCommand(any(), any(), any()) } returns Unit

        useCase(script)

        verify(exactly = 0) { scriptFileRepository.saveToBridge(any(), any()) }

        coVerify {
            termuxRepository.runCommand(
                match { cmd -> cmd.contains("base64 -d") },
                any(),
                any()
            )
        }
    }

    @Test
    fun `invoke handles file write failure gracefully`() = runTest {
        val largeCode = "b".repeat(4005)
        val script = Script(
            id = 2,
            name = "Fail Script",
            code = largeCode,
            interpreter = "bash",
            fileExtension = "sh",
            commandPrefix = "",
            runInBackground = false,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = emptyMap(),
            keepSessionOpen = false
        )

        every {
            scriptFileRepository.saveToBridge(
                any(),
                any()
            )
        } throws RuntimeException("Disk full")

        val commandSlot = slot<String>()
        every { termuxRepository.runCommand(capture(commandSlot), any(), any()) } returns Unit

        useCase(script)

        val cmd = commandSlot.captured
        assertTrue(cmd.contains("echo 'Error: Could not save script"))
    }

    @Test
    fun `invoke assembles all command components in correct order (Small Script)`() = runTest {
        // Arrange
        val script = Script(
            id = 1,
            name = "Complex Config",
            code = "print('hello')",
            interpreter = "python3",
            fileExtension = "py",
            commandPrefix = "sudo nice -n 10",
            executionParams = "--verbose --dry-run",
            envVars = mapOf("API_KEY" to "12345", "MODE" to "prod"),
            runInBackground = true,
            openNewSession = false,
            keepSessionOpen = false,
            iconPath = null
        )

        val commandSlot = slot<String>()
        every { termuxRepository.runCommand(capture(commandSlot), any(), any()) } returns Unit

        useCase(script)

        val fullCommand = commandSlot.captured

        assertTrue("Env vars must be exported", fullCommand.contains("export API_KEY='12345';"))
        assertTrue("Env vars must be exported", fullCommand.contains("export MODE='prod';"))

        assertTrue(
            "Interpreter must follow prefix",
            fullCommand.contains("sudo nice -n 10 python3")
        )

        val interpreterIndex = fullCommand.indexOf("python3")
        val paramsIndex = fullCommand.indexOf("--verbose --dry-run")

        assertTrue("Interpreter was found", interpreterIndex != -1)
        assertTrue("Params were found", paramsIndex != -1)
        assertTrue(
            "Execution params must appear AFTER the interpreter and script path",
            paramsIndex > interpreterIndex
        )
    }

    @Test
    fun `invoke assembles all command components in correct order (Large Script)`() = runTest {
        val largeCode = "a".repeat(4005)
        val script = Script(
            id = 2,
            name = "Large Config",
            code = largeCode,
            interpreter = "node",
            fileExtension = "js",
            commandPrefix = "time",
            executionParams = "server.js",
            envVars = mapOf("PORT" to "8080"),
            runInBackground = true,
            openNewSession = false,
            keepSessionOpen = false,
            iconPath = null
        )

        every { scriptFileRepository.saveToBridge(any(), any()) } returns "/sdcard/fake.js"

        val commandSlot = slot<String>()
        every { termuxRepository.runCommand(capture(commandSlot), any(), any()) } returns Unit

        useCase(script)

        val fullCommand = commandSlot.captured

        assertTrue(fullCommand.contains("export PORT='8080';"))

        assertTrue(
            "Prefix must precede interpreter",
            fullCommand.contains("time node")
        )

        val interpreterIndex = fullCommand.indexOf("node")
        val paramsIndex = fullCommand.indexOf("server.js")

        assertTrue(
            "Params must appear after interpreter",
            paramsIndex > interpreterIndex
        )

        assertTrue(fullCommand.contains("cp -f /sdcard/fake.js"))
    }

    @Test
    fun `invoke correctly escapes Environment Variable values`() = runTest {

        val script = Script(
            id = 3,
            name = "Escape Test",
            code = $$"echo $MSG",
            interpreter = "bash",
            fileExtension = "sh",
            commandPrefix = "",
            executionParams = "",
            envVars = mapOf("MSG" to "Don't stop"),
            runInBackground = true,
            openNewSession = false,
            keepSessionOpen = false,
            iconPath = null
        )

        val commandSlot = slot<String>()
        every { termuxRepository.runCommand(capture(commandSlot), any(), any()) } returns Unit

        useCase(script)

        val fullCommand = commandSlot.captured

        val expected = "export MSG='Don'\\''t stop';"

        assertTrue(
            "Env var with single quotes must be escaped properly. \nGot: $fullCommand",
            fullCommand.contains(expected)
        )
    }
}