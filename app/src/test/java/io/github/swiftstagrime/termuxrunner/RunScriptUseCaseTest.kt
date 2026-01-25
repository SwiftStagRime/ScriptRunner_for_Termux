package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [33])
class RunScriptUseCaseTest {
    private val termuxRepo = mockk<TermuxRepository>(relaxed = true)
    private val fileRepo = mockk<ScriptFileRepository>(relaxed = true)
    private val monitorRepo = mockk<MonitoringRepository>(relaxed = true)

    private lateinit var useCase: RunScriptUseCase
    private val testPackageName = "io.github.swiftstagrime.test"

    @Before
    fun setup() {
        useCase = RunScriptUseCase(testPackageName, termuxRepo, fileRepo, monitorRepo)
    }

    @Test
    fun `small script is encoded as base64 in the command`() =
        runTest {
            val script =
                Script(
                    id = 1,
                    name = "SmallScript",
                    code = "echo 'hello'",
                    interpreter = "bash",
                )

            useCase(script)

            val commandSlot = slot<String>()
            verify {
                termuxRepo.runCommand(
                    command = capture(commandSlot),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            }

            val command = commandSlot.captured
            assertTrue(command.contains("mkdir -p ~/scriptrunner_for_termux"))
            assertTrue(command.contains("base64 -d"))
            assertTrue(command.contains("ZWNobyAnaGVsbG8n"))
        }

    @Test
    fun `large script is saved to bridge repository`() =
        runTest {
            val largeCode = "a".repeat(4001)
            val script = Script(id = 2, name = "LargeScript", code = largeCode)

            coEvery { fileRepo.saveToBridge(any(), any()) } returns "/sdcard/bridge/script_2.sh"

            useCase(script)

            coVerify { fileRepo.saveToBridge(match { it.startsWith("script_2") }, largeCode) }

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue(commandSlot.captured.contains("cp -f /sdcard/bridge/script_2.sh"))
        }

    @Test
    fun `environment variables are sanitized and exported`() =
        runTest {
            val script =
                Script(
                    id = 3,
                    name = "EnvTest",
                    code = "env",
                    envVars = mapOf("VALID_KEY" to "value'with'quote", "123INVALID" to "bad"),
                )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            val command = commandSlot.captured
            assertTrue(command.contains("export VALID_KEY='value'\\''with'\\''quote'"))
            assertFalse(command.contains("123INVALID"))
        }

    @Test
    fun `interpreter maps to correct file extension when extension is blank`() =
        runTest {
            val script =
                Script(
                    id = 4,
                    name = "PyTest",
                    code = "print()",
                    interpreter = "python3",
                    fileExtension = "",
                )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should contain .py extension", commandSlot.captured.contains(".py"))
            assertFalse("Should not contain default .sh extension", commandSlot.captured.contains(".sh"))
        }

    @Test
    fun `heartbeat wrapper is added when enabled`() =
        runTest {
            val script =
                Script(
                    id = 5,
                    name = "HeartbeatTest",
                    code = "sleep 10",
                    useHeartbeat = true,
                    heartbeatInterval = 5000L,
                )
            every { monitorRepo.hasNotificationPermission() } returns true

            useCase(script)

            val commandSlot = slot<String>()
            verify {
                termuxRepo.runCommand(
                    command = capture(commandSlot),
                    runInBackground = any(),
                    sessionAction = any(),
                    scriptId = any(),
                    scriptName = any(),
                    notifyOnResult = any(),
                    automationId = any(),
                )
            }

            val command = commandSlot.captured

            assertTrue(command.contains("am broadcast -a $testPackageName.HEARTBEAT"))
            assertTrue(command.contains("am broadcast -a $testPackageName.SCRIPT_FINISHED"))

            assertTrue(command.contains("HEARTBEAT_PID=$!"))
            assertTrue(command.contains("trap cleanup_heartbeat EXIT"))

            verify { monitorRepo.startMonitoring(script) }
        }

    @Test
    fun `keepSessionOpen appends shell hack`() =
        runTest {
            val script = Script(id = 6, name = "KeepOpen", code = "ls", keepSessionOpen = true)

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue(commandSlot.captured.contains("--- Finished (Press Enter) ---"))
            assertTrue(commandSlot.captured.contains($$"read; exec $SHELL"))
        }

    @Test
    fun `runtimeArgs are correctly appended to script executionParams`() =
        runTest {
            val script = Script(id = 7, name = "ArgTest", code = "ls", executionParams = "-l")

            useCase(script, runtimeArgs = "-a")

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue(commandSlot.captured.contains("-l -a"))
        }

    @Test
    fun `runtime arguments containing double quotes are escaped correctly for bash -c`() =
        runTest {
            val script = Script(id = 8, name = "QuoteTest", code = "ls")
            useCase(script, runtimeArgs = "--name=\"My Script\"")

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            val command = commandSlot.captured
            assertTrue(command.contains("--name=\\\"My Script\\\""))
        }

    @Test
    fun `returns error message command when large script fails to save to bridge`() =
        runTest {
            val script = Script(id = 9, name = "FailTest", code = "a".repeat(4001))

            coEvery { fileRepo.saveToBridge(any(), any()) } throws RuntimeException("Disk Full")

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertEquals("echo 'Error: Could not save script to device storage.'", commandSlot.captured)
        }
}
