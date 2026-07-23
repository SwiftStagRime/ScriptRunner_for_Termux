package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationChain
import io.github.swiftstagrime.termuxrunner.domain.model.ChainCondition
import io.github.swiftstagrime.termuxrunner.domain.model.ChainStep
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ChainStepDto(
    val targetAutomationId: Int,
    val condition: String,
    val envPassThrough: Map<String, String> = emptyMap(),
)

@Entity(
    tableName = "automation_chains",
    foreignKeys = [
        ForeignKey(
            entity = AutomationEntity::class,
            parentColumns = ["id"],
            childColumns = ["triggerAutomationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("triggerAutomationId")],
)
data class AutomationChainEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val triggerAutomationId: Int,
    val steps: String,
)

private val json =
    Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

fun AutomationChainEntity.toDomain(): AutomationChain =
    AutomationChain(
        id = id,
        name = name,
        triggerAutomationId = triggerAutomationId,
        steps = parseSteps(steps),
    )

fun AutomationChain.toChainEntity(): AutomationChainEntity =
    AutomationChainEntity(
        id = id,
        name = name,
        triggerAutomationId = triggerAutomationId,
        steps =
            json.encodeToString<List<ChainStepDto>>(
                steps.map { step ->
                    ChainStepDto(
                        targetAutomationId = step.targetAutomationId,
                        condition = step.condition.name,
                        envPassThrough = step.envPassThrough,
                    )
                },
            ),
    )

private fun parseSteps(jsonString: String): List<ChainStep> {
    if (jsonString.isBlank()) return emptyList()
    return try {
        json.decodeFromString<List<ChainStepDto>>(jsonString).map { dto ->
            ChainStep(
                targetAutomationId = dto.targetAutomationId,
                condition =
                    runCatching { ChainCondition.valueOf(dto.condition) }.getOrDefault(
                        ChainCondition.ON_SUCCESS,
                    ),
                envPassThrough = dto.envPassThrough,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
