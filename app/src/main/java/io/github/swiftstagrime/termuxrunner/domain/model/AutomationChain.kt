package io.github.swiftstagrime.termuxrunner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ChainCondition {
    ON_SUCCESS,
    ON_FAILURE,
    ALWAYS,
}

@Parcelize
data class ChainStep(
    val targetAutomationId: Int,
    val condition: ChainCondition = ChainCondition.ON_SUCCESS,
    val envPassThrough: Map<String, String> = emptyMap(),
) : Parcelable

@Parcelize
data class AutomationChain(
    val id: Int = 0,
    val name: String,
    val triggerAutomationId: Int,
    val steps: List<ChainStep>,
) : Parcelable
