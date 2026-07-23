package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.ExecutionSource
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptExecutionRepository
import io.github.swiftstagrime.termuxrunner.ui.features.executionhistory.ExecutionHistoryViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExecutionHistoryViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ScriptExecutionRepository
    private lateinit var viewModel: ExecutionHistoryViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk<ScriptExecutionRepository>(relaxed = true)
        coEvery { repository.getRecentExecutions(any()) } returns MutableStateFlow(emptyList())
        viewModel = ExecutionHistoryViewModel(repository, testDispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createExecution(
        id: Long = 1L,
        scriptId: Int = 10,
        scriptName: String = "Test",
        timestamp: Long = System.currentTimeMillis(),
        exitCode: Int = 0,
        durationMs: Long? = null,
        source: ExecutionSource = ExecutionSource.MANUAL,
    ) = ScriptExecution(
        id = id,
        scriptId = scriptId,
        scriptName = scriptName,
        timestamp = timestamp,
        exitCode = exitCode,
        durationMs = durationMs,
        runtimeArgs = null,
        source = source,
        errorMessage = null,
    )

    @Test
    fun `uiState initial value has loading true`() {
        val initialState = viewModel.uiState.value
        assertTrue("Initial state should be loading", initialState.isLoading)
    }

    @Test
    fun `clearAll calls repository clearAll`() =
        runTest(testDispatcher) {
            viewModel.clearAll()
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repository.clearAll() }
        }

    @Test
    fun `deleteOldRecords computes correct threshold`() =
        runTest(testDispatcher) {
            viewModel.deleteOldRecords(days = 30)
            testDispatcher.scheduler.advanceUntilIdle()

            val expectedThreshold = System.currentTimeMillis() - (30 * 24L * 60 * 60 * 1000)
            coVerify { repository.deleteOldRecords(match { it >= expectedThreshold - 5000 && it <= expectedThreshold + 5000 }) }
        }

    @Test
    fun `deleteForScript calls repository with correct scriptId`() =
        runTest(testDispatcher) {
            viewModel.deleteForScript(scriptId = 42)
            testDispatcher.scheduler.advanceUntilIdle()

            coVerify { repository.deleteForScript(42) }
        }
}
