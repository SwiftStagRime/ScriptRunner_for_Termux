package io.github.swiftstagrime.termuxrunner.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AutomationType {
    ONE_TIME,
    PERIODIC,
    WEEKLY
}