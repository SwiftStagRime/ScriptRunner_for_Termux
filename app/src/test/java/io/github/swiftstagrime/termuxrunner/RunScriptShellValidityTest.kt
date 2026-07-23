package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.ProcessTermuxResultUseCase
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Integration-style tests: pass real-world Linux commands through the usecase and verify
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [33])
class RunScriptShellValidityTest {
    private val termuxRepo = mockk<TermuxRepository>(relaxed = true)
    private val fileRepo = mockk<ScriptFileRepository>(relaxed = true)
    private val monitorRepo = mockk<MonitoringRepository>(relaxed = true)
    private val processResultTracker = mockk<ProcessTermuxResultUseCase>(relaxed = true)

    private lateinit var useCase: RunScriptUseCase
    private val testPackageName = "io.github.swiftstagrime.test"

    @Before
    fun setup() {
        useCase =
            RunScriptUseCase(
                testPackageName,
                termuxRepo,
                fileRepo,
                monitorRepo,
                processResultTracker,
            )
        every { monitorRepo.hasNotificationPermission() } returns false
    }

    private fun captureCommand(): String {
        val slot = slot<String>()
        verify {
            termuxRepo.runCommand(
                command = capture(slot),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        }
        return slot.captured
    }

    @Test
    fun `bash script with simple echo`() =
        runTest {
            useCase(
                Script(
                    id = 1,
                    name = "t",
                    codePages = listOf("echo hello"),
                    interpreter = "bash",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("bash "))
            assertFalse(cmd.contains("Invalid interpreter"))
        }

    @Test
    fun `python3 script with print`() =
        runTest {
            useCase(
                Script(
                    id = 2,
                    name = "t",
                    codePages = listOf("print('hi')"),
                    interpreter = "python3",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("python3 "))
        }

    @Test
    fun `node script`() =
        runTest {
            useCase(
                Script(
                    id = 3,
                    name = "t",
                    codePages = listOf("console.log(1)"),
                    interpreter = "node",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("node "))
        }

    @Test
    fun `full path interpreter`() =
        runTest {
            useCase(
                Script(
                    id = 4,
                    name = "t",
                    codePages = listOf("echo ok"),
                    interpreter = "/data/data/com.termux/files/usr/bin/python3",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("/data/data/com.termux/files/usr/bin/python3"))
        }

    @Test
    fun `env var with dollar sign`() =
        runTest {
            useCase(
                Script(
                    id = 10,
                    name = "t",
                    codePages = listOf($$"echo $TEST"),
                    envVars = mapOf("TEST" to $$"$HOME/$USER"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export TEST='"))
        }

    @Test
    fun `env var with backticks`() =
        runTest {
            useCase(
                Script(
                    id = 11,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("CMD" to """`whoami`;`id`"""),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export CMD='"))
        }

    @Test
    fun `env var with single quotes`() =
        runTest {
            useCase(
                Script(
                    id = 12,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("MSG" to "it's a test"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("'\\''"))
        }

    @Test
    fun `env var with double quotes`() =
        runTest {
            useCase(
                Script(
                    id = 13,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("MSG" to """he said "hello""""),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export MSG='"))
        }

    @Test
    fun `env var with backslashes`() =
        runTest {
            useCase(
                Script(
                    id = 14,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("PATH" to "C:\\Users\\test"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export PATH='"))
        }

    @Test
    fun `env var with newlines`() =
        runTest {
            useCase(
                Script(
                    id = 15,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("MULTI" to "line1\nline2\r\nline3"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export MULTI='"))
        }

    @Test
    fun `env var with semicolons and pipes`() =
        runTest {
            useCase(
                Script(
                    id = 16,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("SHELL" to "; rm -rf / | cat"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export SHELL='"))
        }

    @Test
    fun `env var with all special chars combined`() =
        runTest {
            useCase(
                Script(
                    id = 17,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("ALL" to """prefix${'$'}HOME `whoami` it's "quoted" \n;|&"""),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export ALL='"))
        }

    @Test
    fun `runtime args with double quotes`() =
        runTest {
            useCase(
                Script(id = 20, name = "t", codePages = listOf("echo \$@")),
                runtimeArgs = "--file=\"my script.sh\"",
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("--file="))
        }

    @Test
    fun `runtime args with dollar and backticks`() =
        runTest {
            val dollarArgs = "prefix" + '$' + "HOME " + "`date`"
            useCase(
                Script(id = 21, name = "t", codePages = listOf("echo \$@")),
                runtimeArgs = dollarArgs,
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("\\\$"))
        }

    @Test
    fun `runtime args with semicolons`() =
        runTest {
            useCase(
                Script(id = 22, name = "t", codePages = listOf("echo \$@")),
                runtimeArgs = "--arg1;--arg2",
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("--arg1"))
        }

    @Test
    fun `prefix sudo`() =
        runTest {
            useCase(
                Script(
                    id = 30,
                    name = "t",
                    codePages = listOf("echo root"),
                    commandPrefix = "sudo",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("sudo"))
        }

    @Test
    fun `prefix with flags`() =
        runTest {
            useCase(
                Script(
                    id = 31,
                    name = "t",
                    codePages = listOf("sleep 10"),
                    commandPrefix = "nice -n 19 ionice -c 3",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("nice"))
            assertTrue(cmd.contains("ionice"))
        }

    @Test
    fun `prefix with dollar and quotes`() =
        runTest {
            useCase(
                Script(
                    id = 32,
                    name = "t",
                    codePages = listOf("echo ok"),
                    commandPrefix = """env FOO="bar${'$'}baz\"""",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("FOO"))
        }

    @Test
    fun `injection via env var value - semicolon command`() =
        runTest {
            useCase(
                Script(
                    id = 40,
                    name = "t",
                    codePages = listOf("echo safe"),
                    envVars = mapOf("X" to "'; rm -rf /; #"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export X='"))
        }

    @Test
    fun `injection via env var value - backtick command`() =
        runTest {
            useCase(
                Script(
                    id = 41,
                    name = "t",
                    codePages = listOf("echo safe"),
                    envVars = mapOf("X" to "`rm -rf /`"),
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("export X='"))
        }

    @Test
    fun `injection via runtime args - semicolon command`() =
        runTest {
            useCase(
                Script(id = 42, name = "t", codePages = listOf("echo \$@")),
                runtimeArgs = "; rm -rf /; echo pwned",
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains(";"))
        }

    @Test
    fun `injection via runtime args - backtick command`() =
        runTest {
            useCase(
                Script(id = 43, name = "t", codePages = listOf("echo \$@")),
                runtimeArgs = """`rm -rf /`""",
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("\\`"))
        }

    @Test
    fun `injection via prefix - command chaining`() =
        runTest {
            useCase(
                Script(
                    id = 44,
                    name = "t",
                    codePages = listOf("echo safe"),
                    commandPrefix = "bash; rm -rf /",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("bash"))
        }

    @Test
    fun `injection via interpreter - already rejected`() =
        runTest {
            useCase(
                Script(
                    id = 45,
                    name = "t",
                    codePages = listOf("echo safe"),
                    interpreter = "python3; rm -rf /",
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("Invalid interpreter"))
        }

    @Test
    fun `large script bridge path with special chars in filename`() =
        runTest {
            val largeCode = "a".repeat(4001)
            coEvery {
                fileRepo.saveToBridge(
                    any(),
                    any(),
                )
            } returns "/sdcard/Download/scriptrunner/script_99_test.sh"

            useCase(Script(id = 50, name = "t", codePages = listOf(largeCode)))
            val cmd = captureCommand()
            assertTrue(cmd.contains("cp -f"))
        }

    @Test
    fun `generated command has balanced outer bash -c quotes`() =
        runTest {
            useCase(
                Script(
                    id = 60,
                    name = "t",
                    codePages = listOf("echo test"),
                    envVars = mapOf("A" to """' " \ $ ${'$'} `"""),
                    interpreter = "bash",
                ),
                runtimeArgs = "arg1 arg2",
            )
            val cmd = captureCommand()
            assertTrue(cmd.startsWith("mkdir -p ~/scriptrunner_for_termux && "))
            assertTrue(cmd.contains("bash -c \""))
            val openIdx = cmd.indexOf("bash -c \"")
            assertTrue(openIdx > 0)
            val afterBashC = cmd.substring(openIdx + 9) // skip "bash -c \""
            assertTrue(
                "Should have closing double-quote for bash -c",
                afterBashC.trimEnd().endsWith("\"") || afterBashC.contains("\";"),
            )
        }

    @Test
    fun `generated command contains bash -c wrapper`() =
        runTest {
            useCase(Script(id = 61, name = "t", codePages = listOf("echo test")))
            val cmd = captureCommand()
            assertTrue(cmd.contains("bash -c \""))
        }

    @Test
    fun `generated command contains cleanup trap`() =
        runTest {
            useCase(Script(id = 62, name = "t", codePages = listOf("echo test")))
            val cmd = captureCommand()
            assertTrue(cmd.contains("trap 'rm -f"))
            assertTrue(cmd.contains("EXIT"))
        }

    @Test
    fun `invalid env key names are rejected`() =
        runTest {
            useCase(
                Script(
                    id = 63,
                    name = "t",
                    codePages = listOf("echo ok"),
                    envVars = mapOf("123BAD" to "x", "ALSO-BAD" to "y", "GOOD_KEY" to "z"),
                ),
            )
            val cmd = captureCommand()
            assertFalse(cmd.contains("export 123BAD"))
            assertFalse(cmd.contains("export ALSO-BAD"))
            assertTrue(cmd.contains("export GOOD_KEY='z'"))
        }

    @Test
    fun `empty script code produces valid command`() =
        runTest {
            useCase(Script(id = 64, name = "t", codePages = listOf("")))
            val cmd = captureCommand()
            assertTrue(cmd.contains("bash -c"))
            assertFalse(cmd.contains("Invalid interpreter"))
        }

    @Test
    fun `script with unicode content`() =
        runTest {
            useCase(
                Script(id = 65, name = "t", codePages = listOf("echo \"Привет мир 🌍\"")),
                runtimeArgs = "日本語テスト",
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("bash -c"))
        }

    @Test
    fun `keep session open appends read prompt`() =
        runTest {
            useCase(
                Script(
                    id = 66,
                    name = "t",
                    codePages = listOf("echo ok"),
                    keepSessionOpen = true,
                ),
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("read"))
            assertTrue(cmd.contains("exec "))
        }

    @Test
    fun `execution params and runtime args combine correctly`() =
        runTest {
            useCase(
                Script(id = 67, name = "t", codePages = listOf("echo"), executionParams = "-x -v"),
                runtimeArgs = "--verbose",
            )
            val cmd = captureCommand()
            assertTrue(cmd.contains("-x"))
            assertTrue(cmd.contains("-v"))
            assertTrue(cmd.contains("--verbose"))
        }
}
