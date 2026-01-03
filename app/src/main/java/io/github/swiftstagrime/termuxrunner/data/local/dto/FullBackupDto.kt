package io.github.swiftstagrime.termuxrunner.data.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class FullBackupDto(
    val version: Int = 2,
    val categories: List<CategoryExportDto>,
    val scripts: List<ScriptExportDto>
)