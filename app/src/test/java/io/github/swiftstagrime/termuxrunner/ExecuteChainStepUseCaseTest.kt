package io.github.swiftstagrime.termuxrunner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationChain
import io.github.swiftstagrime.termuxrunner.domain.model.ChainCondition
import io.github.swiftstagrime.termuxrunner.domain.model.ChainStep
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationChainRepository
import io.github.swiftstagrime.termuxrunner.domain.usecase.ExecuteChainStepUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The chain editor keeps the trigger in [AutomationChain.triggerAutomationId] and stores
 * only the follow-up steps, so the step list always starts with the step to run next.
 * These tests pin that contract: a chain must never silently drop its first step.
 */
@Config(application = TestApplication::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class ExecuteChainStepUseCaseTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val chainRepository = mockk<AutomationChainRepository>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val useCase = ExecuteChainStepUseCase(chainRepository, context)

    @Before
    fun setup() {
        // getInstance lives on the companion, so the companion is what must be stubbed
        mockkObject(WorkManager.Companion)
        every { WorkManager.getInstance(any<Context>()) } returns workManager
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `first follow-up step is dispatched, not the second one`() {
        val dispatched =
            dispatchedAutomationIds(
                steps = listOf(step(2, ChainCondition.ON_SUCCESS), step(3, ChainCondition.ON_SUCCESS)),
                exitCode = 0,
            )

        assertEquals(listOf(2), dispatched)
    }

    @Test
    fun `single step chain dispatches its only step`() {
        val dispatched =
            dispatchedAutomationIds(
                steps = listOf(step(2, ChainCondition.ON_SUCCESS)),
                exitCode = 0,
            )

        assertEquals(listOf(2), dispatched)
    }

    @Test
    fun `failed run dispatches the on-failure step`() {
        val dispatched =
            dispatchedAutomationIds(
                steps = listOf(step(2, ChainCondition.ON_FAILURE)),
                exitCode = 1,
            )

        assertEquals(listOf(2), dispatched)
    }

    @Test
    fun `chain that still stores the trigger as its first step skips only that step`() {
        val dispatched =
            dispatchedAutomationIds(
                steps = listOf(step(TRIGGER, ChainCondition.ALWAYS), step(4, ChainCondition.ALWAYS)),
                exitCode = 0,
            )

        assertEquals(listOf(4), dispatched)
    }

    @Test
    fun `nothing is dispatched when no step matches the exit code`() {
        val dispatched =
            dispatchedAutomationIds(
                steps = listOf(step(2, ChainCondition.ON_FAILURE)),
                exitCode = 0,
            )

        assertEquals(emptyList<Int>(), dispatched)
    }

    /** Runs the use case for [TRIGGER] and returns the automation ids of the enqueued workers. */
    private fun dispatchedAutomationIds(
        steps: List<ChainStep>,
        exitCode: Int,
    ): List<Int> {
        coEvery { chainRepository.getChainsByTriggerId(TRIGGER) } returns
            listOf(
                AutomationChain(
                    id = 7,
                    name = "chain",
                    triggerAutomationId = TRIGGER,
                    steps = steps,
                ),
            )

        val enqueued = mutableListOf<OneTimeWorkRequest>()
        runBlocking { useCase(TRIGGER, exitCode) }
        verify(atLeast = 0) { workManager.enqueue(capture(enqueued)) }

        return enqueued.map { it.workSpec.input.getInt("automation_id", -1) }
    }

    private fun step(
        targetAutomationId: Int,
        condition: ChainCondition,
    ) = ChainStep(targetAutomationId = targetAutomationId, condition = condition)

    companion object {
        private const val TRIGGER = 1
    }
}
