package io.github.swiftstagrime.termuxrunner

import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxPermissionException
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.RunScriptUseCase
import io.github.swiftstagrime.termuxrunner.ui.features.runner.ScriptRunnerEvent
import io.github.swiftstagrime.termuxrunner.ui.features.runner.ScriptRunnerViewModel
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScriptRunnerTest {

    @MockK
    lateinit var termuxRepository: TermuxRepository

    @MockK
    lateinit var scriptFileRepository: ScriptFileRepository

    @MockK
    lateinit var scriptRepository: ScriptRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Base64::class)

        every {
            Base64.encodeToString(any<ByteArray>(), any())
        } answers { call ->
            val bytes = call.invocation.args[0] as ByteArray
            java.util.Base64.getEncoder().encodeToString(bytes)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `RunScriptUseCase generates correct bash command for small scripts`() = runTest {
        val script = Script(
            id = 1,
            name = "Test",
            code = "echo 'Hello'",
            interpreter = "bash",
            fileExtension = "sh",
            commandPrefix = "",
            runInBackground = true,
            openNewSession = false,
            executionParams = "",
            iconPath = null,
            envVars = mapOf("API_KEY" to "123"),
            keepSessionOpen = false
        )

        val commandSlot = slot<String>()
        every { termuxRepository.runCommand(capture(commandSlot), any(), any()) } returns Unit

        val useCase = RunScriptUseCase(termuxRepository, scriptFileRepository)

        useCase(script)

        val capturedCommand = commandSlot.captured

        assertTrue(capturedCommand.contains("export API_KEY='123'"))

        assertTrue(capturedCommand.contains("mkdir -p ~/scriptrunner_for_termux"))

        assertTrue(capturedCommand.contains("base64 -d"))

        assertTrue(capturedCommand.contains("rm -f"))
    }

    @Test
    fun `ViewModel executes script and finishes when permission granted`() = runTest {
        val scriptId = 1
        val savedStateHandle = SavedStateHandle(mapOf("SCRIPT_ID" to scriptId))
        val script = mockk<Script>(relaxed = true) {
            every { code } returns "echo test"
            every { interpreter } returns "bash"
        }

        coEvery { scriptRepository.getScriptById(scriptId) } returns script
        every { termuxRepository.runCommand(any(), any(), any()) } returns Unit

        val useCase = RunScriptUseCase(
            termuxRepository,
            scriptFileRepository
        )
        val viewModel = ScriptRunnerViewModel(scriptRepository, useCase, savedStateHandle)

        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is ScriptRunnerEvent.Finish)

        coVerify { termuxRepository.runCommand(any(), any(), any()) }
    }

    @Test
    fun `ViewModel emits RequestPermission when permission denied`() = runTest {
        val scriptId = 1
        val savedStateHandle = SavedStateHandle(mapOf("SCRIPT_ID" to scriptId))
        val script = mockk<Script>(relaxed = true)

        coEvery { scriptRepository.getScriptById(scriptId) } returns script

        every {
            termuxRepository.runCommand(
                any(),
                any(),
                any()
            )
        } throws TermuxPermissionException()

        val useCase = RunScriptUseCase(termuxRepository, scriptFileRepository)
        val viewModel = ScriptRunnerViewModel(scriptRepository, useCase, savedStateHandle)

        testDispatcher.scheduler.advanceUntilIdle()

        val event = viewModel.events.first()
        assertTrue(event is ScriptRunnerEvent.RequestPermission)
    }
}