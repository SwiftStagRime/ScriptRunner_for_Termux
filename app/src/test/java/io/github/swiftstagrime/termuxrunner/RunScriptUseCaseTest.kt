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
                    codePages = listOf("echo 'hello'"),
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
            val script = Script(id = 2, name = "LargeScript", codePages = listOf(largeCode))

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
                    codePages = listOf("env"),
                    envVars = mapOf("VALID_KEY" to "value'with'quote", "123INVALID" to "bad"),
                )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            val command = commandSlot.captured
            assertTrue("Should contain escaped export statement", command.contains("export VALID_KEY='value'\\''with'\\''quote'"))
            assertFalse(command.contains("123INVALID"))
        }

    @Test
    fun `interpreter maps to correct file extension when extension is blank`() =
        runTest {
            val script =
                Script(
                    id = 4,
                    name = "PyTest",
                    codePages = listOf("print()"),
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
    fun `TCP wrapper is added when port available`() =
        runTest {
            val script =
                Script(
                    id = 5,
                    name = "TcpTest",
                    codePages = listOf("sleep 10"),
                    useHeartbeat = true,
                    heartbeatInterval = 5000L,
                )
            every { monitorRepo.hasNotificationPermission() } returns true
            every { monitorRepo.startMonitoring(script) } returns 8765

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

            assertTrue(command.contains("socket.AF_INET"))
            assertTrue(command.contains("127.0.0.1"))
            assertTrue(command.contains("8765"))
            assertTrue(command.contains("EXIT_OK"))
            assertTrue(command.contains("python3"))

            verify { monitorRepo.startMonitoring(script) }
        }

    @Test
    fun `broadcast heartbeat wrapper used as fallback when port is null`() =
        runTest {
            val script =
                Script(
                    id = 6,
                    name = "HeartbeatFallback",
                    codePages = listOf("sleep 10"),
                    useHeartbeat = true,
                    heartbeatInterval = 5000L,
                )
            every { monitorRepo.hasNotificationPermission() } returns true
            every { monitorRepo.startMonitoring(script) } returns null

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
            val script = Script(id = 6, name = "KeepOpen", codePages = listOf("ls"), keepSessionOpen = true)

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue(commandSlot.captured.contains("--- Finished (Press Enter) ---"))
            assertTrue(commandSlot.captured.contains($$"read; exec $SHELL"))
        }

    @Test
    fun `runtimeArgs are correctly appended to script executionParams`() =
        runTest {
            val script = Script(id = 7, name = "ArgTest", codePages = listOf("ls"), executionParams = "-l")

            useCase(script, runtimeArgs = "-a")

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue(commandSlot.captured.contains("-l -a"))
        }

    @Test
    fun `runtime arguments containing double quotes are escaped correctly for bash -c`() =
        runTest {
            val script = Script(id = 8, name = "QuoteTest", codePages = listOf("ls"))
            useCase(script, runtimeArgs = "--name=\"My Script\"")

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            val command = commandSlot.captured
            assertTrue(command.contains("--name=\\\"My Script\\\""))
        }

    @Test
    fun `returns error message command when large script fails to save to bridge`() =
        runTest {
            val script = Script(id = 9, name = "FailTest", codePages = listOf("a".repeat(4001)))

            coEvery { fileRepo.saveToBridge(any(), any()) } throws RuntimeException("Disk Full")

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertEquals("echo 'Error: Could not save script to device storage.'", commandSlot.captured)
        }

    @Test
    fun `malicious interpreter with semicolon is rejected`() =
        runTest {
            val script = Script(
                id = 100,
                name = "Inject",
                codePages = listOf("echo hello"),
                interpreter = "bash; rm -rf /",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should return error, not execute malicious command",
                commandSlot.captured.contains("Invalid interpreter"))
        }

    @Test
    fun `malicious interpreter with backtick is rejected`() =
        runTest {
            val script = Script(
                id = 101,
                name = "Inject2",
                codePages = listOf("echo hello"),
                interpreter = "`rm -rf /`",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should return error for backtick injection",
                commandSlot.captured.contains("Invalid interpreter"))
            assertFalse(commandSlot.captured.contains("`rm -rf /`"))
        }

    @Test
    fun `malicious interpreter with dollar sign is rejected`() =
        runTest {
            val script = Script(
                id = 102,
                name = "Inject3",
                codePages = listOf("echo hello"),
                interpreter = "$(rm -rf /)",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should return error for dollar-sign injection",
                commandSlot.captured.contains("Invalid interpreter"))
        }

    @Test
    fun `malicious interpreter with pipe is rejected`() =
        runTest {
            val script = Script(
                id = 103,
                name = "Inject4",
                codePages = listOf("echo hello"),
                interpreter = "bash | rm -rf /",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should return error for pipe injection",
                commandSlot.captured.contains("Invalid interpreter"))
        }

    @Test
    fun `malicious interpreter with ampersand is rejected`() =
        runTest {
            val script = Script(
                id = 104,
                name = "Inject5",
                codePages = listOf("echo hello"),
                interpreter = "bash & rm -rf /",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should return error for ampersand injection",
                commandSlot.captured.contains("Invalid interpreter"))
        }

    @Test
    fun `valid path-based interpreter is accepted`() =
        runTest {
            val script = Script(
                id = 105,
                name = "ValidPath",
                codePages = listOf("echo hello"),
                interpreter = "/data/data/com.termux/files/usr/bin/bash",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertFalse("Should not be rejected as invalid",
                commandSlot.captured.contains("Invalid interpreter"))
            assertTrue("Should contain the valid interpreter path",
                commandSlot.captured.contains("/data/data/com.termux/files/usr/bin/bash"))
        }

    @Test
    fun `env var value with dollar sign is safe inside single quotes`() =
        runTest {
            val dollarValue = "prefix" + '$' + "HOMEsuffix"
            val script = Script(
                id = 106,
                name = "EnvDollar",
                codePages = listOf("echo test"),
                envVars = mapOf("TEST_VAR" to dollarValue),
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Dollar sign should appear literally (safe inside single quotes)",
                commandSlot.captured.contains("'prefix\$HOMEsuffix'"))
        }

    @Test
    fun `env var value with backtick is safe inside single quotes`() =
        runTest {
            val script = Script(
                id = 107,
                name = "EnvBacktick",
                codePages = listOf("echo test"),
                envVars = mapOf("TEST_VAR" to """prefix`whoami`suffix"""),
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Backtick should appear literally (safe inside single quotes)",
                commandSlot.captured.contains("`whoami`"))
        }

    @Test
    fun `env var value with double quote is not specially handled in single-quote context`() =
        runTest {
            val script = Script(
                id = 108,
                name = "EnvQuote",
                codePages = listOf("echo test"),
                envVars = mapOf("TEST_VAR" to "value'with'quotes"),
            )

            useCase(script)

            // Just verify it doesn't crash and produces valid output
            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Should contain the export statement",
                commandSlot.captured.contains("export TEST_VAR="))
        }

    @Test
    fun `interpreter allowlist rejects spaces`() =
        runTest {
            val script = Script(
                id = 109,
                name = "SpaceInject",
                codePages = listOf("echo hello"),
                interpreter = "bash -c 'rm -rf /'",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Spaces in interpreter should be rejected",
                commandSlot.captured.contains("Invalid interpreter"))
        }

    @Test
    fun `interpreter allowlist rejects newlines`() =
        runTest {
            val script = Script(
                id = 110,
                name = "NewlineInject",
                codePages = listOf("echo hello"),
                interpreter = "bash\nrm -rf /",
            )

            useCase(script)

            val commandSlot = slot<String>()
            verify { termuxRepo.runCommand(command = capture(commandSlot), any(), any(), any(), any(), any(), any()) }

            assertTrue("Newlines in interpreter should be rejected",
                commandSlot.captured.contains("Invalid interpreter"))
        }
}
