package io.github.swiftstagrime.termuxrunner.domain.usecase

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.worker.AutomationWorker
import io.github.swiftstagrime.termuxrunner.domain.model.ChainCondition
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationChainRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExecuteChainStepUseCase
    @Inject
    constructor(
        private val chainRepository: AutomationChainRepository,
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val CHAIN_OUTPUT_DIR = "scriptrunner_chains"
        }

        suspend operator fun invoke(
            completedAutomationId: Int,
            exitCode: Int,
        ) {
            withContext(Dispatchers.IO) {
                val chains = chainRepository.getChainsByTriggerId(completedAutomationId)
                if (chains.isEmpty()) return@withContext

                for (chain in chains) {
                    val steps = chain.steps
                    if (steps.size < 2) continue

                    val triggerStepIndex =
                        steps.indexOfFirst { it.targetAutomationId == completedAutomationId }
                    val startIndex = if (triggerStepIndex >= 0) triggerStepIndex + 1 else 1
                    if (startIndex >= steps.size) continue

                    val matchedConditions =
                        when (exitCode) {
                            0 -> listOf(ChainCondition.ON_SUCCESS, ChainCondition.ALWAYS)
                            else -> listOf(ChainCondition.ON_FAILURE, ChainCondition.ALWAYS)
                        }

                    val nextStep =
                        steps.drop(startIndex).firstOrNull { it.condition in matchedConditions }
                            ?: continue

                    val outputContent = readPreviousOutput(completedAutomationId)

                    val dataBuilder =
                        Data
                            .Builder()
                            .apply {
                                putInt("automation_id", nextStep.targetAutomationId)
                                putString("chain_step_env_PREV_OUTPUT", outputContent.take(4096))
                                putString("chain_step_env_PREV_EXIT_CODE", exitCode.toString())
                                for ((key, value) in nextStep.envPassThrough) {
                                    putString(
                                        "chain_step_env_$key",
                                        value.replace("\${PREV_OUTPUT}", outputContent.take(1024)),
                                    )
                                }
                            }.build()

                    val workRequest =
                        OneTimeWorkRequestBuilder<AutomationWorker>()
                            .setInputData(dataBuilder)
                            .build()

                    WorkManager.getInstance(context).enqueue(workRequest)
                }
            }
        }

        private fun readPreviousOutput(automationId: Int): String {
            val outputDir = File(context.filesDir, CHAIN_OUTPUT_DIR)
            val outputFile = File(outputDir, "output_$automationId.txt")
            return try {
                if (outputFile.exists()) outputFile.readText() else ""
            } catch (_: Exception) {
                ""
            }
        }
    }
