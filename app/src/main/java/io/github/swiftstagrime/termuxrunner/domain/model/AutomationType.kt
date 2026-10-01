package io.github.swiftstagrime.termuxrunner.domain.model

import kotlinx.serialization.Serializable

enum class TriggerMode {
    SCHEDULE,
    EVENT,
    BOOT,
}

/**
 * Automation trigger types.
 *
 * Note: the network/USB event triggers were removed — the OS does not deliver
 * CONNECTIVITY_CHANGE / USB_DEVICE_ATTACHED to manifest receivers on API 26+,
 * and a runtime NetworkCallback only lives while the process does, which is
 * unreliable for a companion app that is not resident. Old exports/backups
 * referencing them are skipped on import, and old database rows are purged in
 * MIGRATION_9_10.
 */
@Serializable
enum class AutomationType(
    val isEventBased: Boolean = false,
) {
    ONE_TIME,
    PERIODIC,
    WEEKLY,
    BOOT,
    MONTHLY,
    TIME_WINDOW,
    RANDOM_DELAY,
    SCREEN_ON(isEventBased = true),
    SCREEN_OFF(isEventBased = true),
}

val AutomationType.triggerMode: TriggerMode
    get() =
        when (this) {
            AutomationType.ONE_TIME,
            AutomationType.PERIODIC,
            AutomationType.WEEKLY,
            AutomationType.MONTHLY,
            AutomationType.TIME_WINDOW,
            AutomationType.RANDOM_DELAY,
            -> TriggerMode.SCHEDULE

            AutomationType.BOOT -> TriggerMode.BOOT

            AutomationType.SCREEN_ON,
            AutomationType.SCREEN_OFF,
            -> TriggerMode.EVENT
        }

val TriggerMode.availableTypes: List<AutomationType>
    get() =
        when (this) {
            TriggerMode.SCHEDULE ->
                listOf(
                    AutomationType.ONE_TIME,
                    AutomationType.PERIODIC,
                    AutomationType.WEEKLY,
                    AutomationType.MONTHLY,
                    AutomationType.TIME_WINDOW,
                    AutomationType.RANDOM_DELAY,
                )

            TriggerMode.EVENT ->
                listOf(
                    AutomationType.SCREEN_ON,
                    AutomationType.SCREEN_OFF,
                )

            TriggerMode.BOOT -> listOf(AutomationType.BOOT)
        }
