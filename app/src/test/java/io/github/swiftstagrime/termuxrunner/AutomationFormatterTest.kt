package io.github.swiftstagrime.termuxrunner

import android.content.Context
import android.os.BatteryManager
import androidx.test.core.app.ApplicationProvider
import io.github.swiftstagrime.termuxrunner.domain.model.Automation
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationFormatter
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(application = TestApplication::class, sdk = [34])
@RunWith(RobolectricTestRunner::class)
class AutomationFormatterTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        // Deterministic power state: not charging, high level
        val shadowBattery = shadowOf(context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        shadowBattery.setIsCharging(false)
        shadowBattery.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 100)
    }

    @Test
    fun `future next run shows relative time`() {
        val result =
            AutomationFormatter.formatNextRun(
                context,
                automation(nextRunTimestamp = System.currentTimeMillis() + 60 * 60 * 1000),
            )
        assertEquals(
            true,
            result.startsWith(context.getString(R.string.automation_next_run_format, "")),
        )
    }

    @Test
    fun `null next run shows not scheduled`() {
        assertEquals(
            context.getString(R.string.automation_not_scheduled),
            AutomationFormatter.formatNextRun(context, automation(nextRunTimestamp = null)),
        )
    }

    @Test
    fun `pending run with all conditions met shows running soon`() {
        assertEquals(
            context.getString(R.string.automation_running_soon),
            AutomationFormatter.formatNextRun(
                context,
                automation(nextRunTimestamp = System.currentTimeMillis() - 60 * 1000),
            ),
        )
    }

    @Test
    fun `pending run without wifi shows waiting for wifi`() {
        assertEquals(
            context.getString(R.string.automation_waiting_for, context.getString(R.string.automation_wait_wifi)),
            AutomationFormatter.formatNextRun(
                context,
                automation(
                    nextRunTimestamp = System.currentTimeMillis() - 60 * 1000,
                    requireWifi = true,
                ),
            ),
        )
    }

    @Test
    fun `pending run without charger shows waiting for charger`() {
        assertEquals(
            context.getString(R.string.automation_waiting_for, context.getString(R.string.automation_wait_charging)),
            AutomationFormatter.formatNextRun(
                context,
                automation(
                    nextRunTimestamp = System.currentTimeMillis() - 60 * 1000,
                    requireCharging = true,
                ),
            ),
        )
    }

    @Test
    fun `pending run below battery threshold shows waiting for battery level`() {
        shadowOf(context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
            .setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 50)
        assertEquals(
            context.getString(R.string.automation_waiting_for, context.getString(R.string.automation_wait_battery)),
            AutomationFormatter.formatNextRun(
                context,
                automation(
                    nextRunTimestamp = System.currentTimeMillis() - 60 * 1000,
                    batteryThreshold = 80,
                ),
            ),
        )
    }

    @Test
    fun `pending run with wifi and charger missing lists both`() {
        assertEquals(
            context.getString(
                R.string.automation_waiting_for,
                "${context.getString(R.string.automation_wait_wifi)}, " +
                    context.getString(R.string.automation_wait_charging),
            ),
            AutomationFormatter.formatNextRun(
                context,
                automation(
                    nextRunTimestamp = System.currentTimeMillis() - 60 * 1000,
                    requireWifi = true,
                    requireCharging = true,
                ),
            ),
        )
    }

    @Test
    fun `pending run while charging shows running soon`() {
        val shadowBattery = shadowOf(context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        shadowBattery.setIsCharging(true)
        shadowBattery.setIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY, 80)
        assertEquals(
            context.getString(R.string.automation_running_soon),
            AutomationFormatter.formatNextRun(
                context,
                automation(
                    nextRunTimestamp = System.currentTimeMillis() - 60 * 1000,
                    requireCharging = true,
                ),
            ),
        )
    }

    private fun automation(
        nextRunTimestamp: Long?,
        requireWifi: Boolean = false,
        requireCharging: Boolean = false,
        batteryThreshold: Int = 0,
    ) = Automation(
        id = 1,
        scriptId = 1,
        label = "Test",
        type = AutomationType.PERIODIC,
        scheduledTimestamp = System.currentTimeMillis(),
        intervalMillis = 60 * 60 * 1000,
        daysOfWeek = emptyList(),
        isEnabled = true,
        lastRunTimestamp = null,
        nextRunTimestamp = nextRunTimestamp,
        requireWifi = requireWifi,
        requireCharging = requireCharging,
        batteryThreshold = batteryThreshold,
    )
}
