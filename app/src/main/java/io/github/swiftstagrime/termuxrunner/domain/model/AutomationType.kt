package io.github.swiftstagrime.termuxrunner.domain.model

import kotlinx.serialization.Serializable

enum class TriggerMode {
    SCHEDULE,
    EVENT,
    BOOT,
}

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
    NETWORK_CONNECTED(isEventBased = true),
    NETWORK_DISCONNECTED(isEventBased = true),
    USB_CONNECTED(isEventBased = true),
    USB_DISCONNECTED(isEventBased = true),
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
            AutomationType.NETWORK_CONNECTED,
            AutomationType.NETWORK_DISCONNECTED,
            AutomationType.USB_CONNECTED,
            AutomationType.USB_DISCONNECTED,
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
                    AutomationType.NETWORK_CONNECTED,
                    AutomationType.NETWORK_DISCONNECTED,
                    AutomationType.USB_CONNECTED,
                    AutomationType.USB_DISCONNECTED,
                )

            TriggerMode.BOOT -> listOf(AutomationType.BOOT)
        }
