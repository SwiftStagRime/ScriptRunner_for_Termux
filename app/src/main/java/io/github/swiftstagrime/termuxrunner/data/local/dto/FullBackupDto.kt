package io.github.swiftstagrime.termuxrunner.data.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class FullBackupDto(
    val version: Int = 6,
    val categories: List<CategoryExportDto> = emptyList(),
    val scripts: List<ScriptExportDto>,
    val automations: List<AutomationExportDto> = emptyList(),
    val themes: List<CustomThemeExportDto> = emptyList(),
    val chains: List<AutomationChainExportDto> = emptyList(),
)

@Serializable
data class AutomationChainExportDto(
    val name: String,
    val triggerAutomationId: Int,
    val steps: List<AutomationChainStepExportDto>,
)

@Serializable
data class AutomationChainStepExportDto(
    val targetAutomationId: Int,
    val condition: String = "ON_SUCCESS",
    val envPassThrough: Map<String, String> = emptyMap(),
)
