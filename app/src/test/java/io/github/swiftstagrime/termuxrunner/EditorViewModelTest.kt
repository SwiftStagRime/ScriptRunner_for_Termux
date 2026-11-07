package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import io.github.swiftstagrime.termuxrunner.ui.features.editor.EditorViewModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var scriptRepository: ScriptRepository
    private lateinit var viewModel: EditorViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        scriptRepository = mockk(relaxed = true)
        val automationRepository = mockk<AutomationRepository>(relaxed = true)
        val categoryRepository = mockk<CategoryRepository>(relaxed = true)
        val userPreferencesRepository = mockk<UserPreferencesRepository>(relaxed = true)
        every { automationRepository.getAllAutomations() } returns flowOf(emptyList())
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())
        every { userPreferencesRepository.editorLineWrappingEnabled } returns flowOf(false)

        viewModel =
            EditorViewModel(
                scriptRepository = scriptRepository,
                automationRepository = automationRepository,
                categoryRepository = categoryRepository,
                userPreferencesRepository = userPreferencesRepository,
                updateScriptUseCase = mockk(relaxed = true),
                iconRepository = mockk(relaxed = true),
                widgetManager = mockk(relaxed = true),
                ioDispatcher = testDispatcher,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun savedScript() =
        Script(
            id = 5,
            name = "Saved Name",
            codePages = listOf("echo old"),
            pageNames = listOf("Main"),
            interpreter = "bash",
        )

    @Test
    fun reloadScript_preservesUnsavedEdits() =
        runTest(testDispatcher) {
            coEvery { scriptRepository.getScriptById(5) } returns savedScript()
            viewModel.loadScript(5)
            advanceUntilIdle()
            assertEquals(listOf("echo old"), viewModel.currentScript.value?.codePages)

            viewModel.updateCurrentPageCode("echo typed")

            viewModel.reloadScript()
            advanceUntilIdle()

            assertEquals(listOf("echo typed"), viewModel.currentScript.value?.codePages)
            assertEquals(listOf("Main"), viewModel.currentScript.value?.pageNames)
        }

    @Test
    fun reloadScript_refreshesNonCodeFieldsFromDatabase() =
        runTest(testDispatcher) {
            coEvery { scriptRepository.getScriptById(5) } returns savedScript()
            viewModel.loadScript(5)
            advanceUntilIdle()

            viewModel.updateCurrentPageCode("echo typed")
            coEvery { scriptRepository.getScriptById(5) } returns
                savedScript().copy(name = "Renamed", interpreter = "python3")

            viewModel.reloadScript()
            advanceUntilIdle()

            assertEquals("Renamed", viewModel.currentScript.value?.name)
            assertEquals("python3", viewModel.currentScript.value?.interpreter)
            assertEquals(listOf("echo typed"), viewModel.currentScript.value?.codePages)
        }
}
