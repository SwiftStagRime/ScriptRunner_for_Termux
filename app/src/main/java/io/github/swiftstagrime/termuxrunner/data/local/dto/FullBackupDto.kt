package io.github.swiftstagrime.termuxrunner.data.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class FullBackupDto(
    val version: Int = 3,
    val categories: List<CategoryExportDto> = emptyList(),
    val scripts: List<ScriptExportDto>,
    val automations: List<AutomationExportDto> = emptyList(),
)
