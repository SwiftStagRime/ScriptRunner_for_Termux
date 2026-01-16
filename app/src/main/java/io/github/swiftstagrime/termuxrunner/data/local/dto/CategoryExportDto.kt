package io.github.swiftstagrime.termuxrunner.data.local.dto

import kotlinx.serialization.Serializable

@Serializable
data class CategoryExportDto(
    val id: Int,
    val name: String,
    val orderIndex: Int,
)
