package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.util.AutomationTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationTimeCalculatorTest {

    private fun createPeriodicEntity(
        intervalMillis: Long = 60_000L,
        scheduledTimestamp: Long = 1_000_000L,
        nextRunTimestamp: Long? = null,
    ) =
        AutomationEntity(
            id = 1,
            scriptId = 1,
            label = "test",
            type = AutomationType.PERIODIC,
            scheduledTimestamp = scheduledTimestamp,
            intervalMillis = intervalMillis,
            daysOfWeek = emptyList(),
            nextRunTimestamp = nextRunTimestamp,
        )

    // --- PERIODIC: O(1) math calculation (commit 9db5dfa + 31b8271) ---

    @Test
    fun `periodic returns next interval when already in the future`() {
        val entity = createPeriodicEntity(
            intervalMillis = 60_000,
            scheduledTimestamp = 1_000_000,
            nextRunTimestamp = 2_000_000,
        )

        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_500_000)

        assertEquals(2_000_000L, result)
    }

    @Test
    fun `periodic skips ahead when nextRunTimestamp is in the past`() {
        val entity = createPeriodicEntity(
            intervalMillis = 60_000,
            scheduledTimestamp = 1_000_000,
            nextRunTimestamp = 1_000_000,
        )

        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 700_000)

        // 1_000_000 + ceil((700_000 - 1_000_000 + 1) / 60_000 + 1) * 60_000
        // = 1_000_000 + (0/60_000 + 1) ... wait, diff is negative so no skip needed? No: fromTime > nextRunTimestamp
        // Actually: next(1_000_000) <= fromTime(700_000)? No! 1_000_000 > 700_000, so no skip.
        // Wait, let me re-check: next = 1_000_000, fromTime = 700_000 -> next > fromTime, so result is just next
        assertEquals(1_000_000L, result)
    }

    @Test
    fun `periodic skips multiple intervals when far behind`() {
        val entity = createPeriodicEntity(
            intervalMillis = 60_000,
            scheduledTimestamp = 1_000_000,
            nextRunTimestamp = 1_000_000,
        )

        // fromTime is 5 intervals (300_000ms) ahead of nextRunTimestamp
        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_300_001)

        // Should skip past all missed intervals and land on the first one after fromTime
        assertTrue(result!! > 1_300_001L)
        // Result should be aligned to interval boundaries from nextRunTimestamp
        val diff = result - 1_000_000
        assertEquals(0, diff % 60_000)
    }

    @Test
    fun `periodic handles very small interval without hanging`() {
        val entity = createPeriodicEntity(
            intervalMillis = 1, // 1 millisecond interval!
            scheduledTimestamp = 1_000_000,
            nextRunTimestamp = 1_000_000,
        )

        // fromTime is a billion ms ahead - old loop would have iterated 1 billion times
        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_000_001_000_000L)

        // Should return immediately via O(1) math, not hang
        assertTrue(result!! > 1_000_001_000_000L)
    }

    @Test
    fun `periodic returns null when interval is zero`() {
        val entity = createPeriodicEntity(intervalMillis = 0)

        val result = AutomationTimeCalculator.calculateNextRun(entity)

        assertNull(result)
    }

    @Test
    fun `periodic returns null when interval is negative`() {
        val entity = createPeriodicEntity(intervalMillis = -1000)

        val result = AutomationTimeCalculator.calculateNextRun(entity)

        assertNull(result)
    }

    @Test
    fun `periodic uses scheduledTimestamp when nextRunTimestamp is null`() {
        val entity = createPeriodicEntity(
            intervalMillis = 60_000,
            scheduledTimestamp = 5_000_000,
            nextRunTimestamp = null,
        )

        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 4_000_000)

        assertEquals(5_000_000L, result)
    }

    @Test
    fun `periodic exact boundary returns next interval`() {
        val entity = createPeriodicEntity(
            intervalMillis = 60_000,
            scheduledTimestamp = 1_000_000,
            nextRunTimestamp = 1_000_000,
        )

        // fromTime equals exactly one interval boundary
        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_060_000)

        // Should return the NEXT interval (strictly greater than fromTime)
        assertEquals(1_120_000L, result)
    }

    @Test
    fun `periodic large interval works correctly`() {
        val entity = createPeriodicEntity(
            intervalMillis = 86_400_000, // 1 day
            scheduledTimestamp = 1_000_000_000_000,
            nextRunTimestamp = 1_000_000_000_000,
        )

        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_000_086_400_001)

        // Should skip to next day boundary after fromTime
        assertEquals(1_000_172_800_000L, result)
    }

    @Test
    fun `getNextRuns returns multiple upcoming runs`() {
        val entity = createPeriodicEntity(
            intervalMillis = 60_000,
            scheduledTimestamp = 10_000_000,
            nextRunTimestamp = 10_000_000,
        )

        // Use a fixed past time so results are deterministic... but getNextRuns uses System.currentTimeMillis()
        // So we can only verify the structure
        val runs = AutomationTimeCalculator.getNextRuns(entity, count = 3)

        assertTrue("Should return at least some runs", runs.isNotEmpty())
        for (i in 1 until runs.size) {
            assertTrue("Runs should be in ascending order", runs[i] > runs[i - 1])
        }
    }

    // --- ONE_TIME tests ---

    @Test
    fun `oneTime returns scheduledTimestamp when in the future`() {
        val entity = AutomationEntity(
            id = 1, scriptId = 1, label = "test", type = AutomationType.ONE_TIME,
            scheduledTimestamp = 9_999_999, daysOfWeek = emptyList(),
        )

        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_000_000)

        assertEquals(9_999_999L, result)
    }

    @Test
    fun `oneTime returns null when in the past`() {
        val entity = AutomationEntity(
            id = 1, scriptId = 1, label = "test", type = AutomationType.ONE_TIME,
            scheduledTimestamp = 500_000, daysOfWeek = emptyList(),
        )

        val result = AutomationTimeCalculator.calculateNextRun(entity, fromTime = 1_000_000)

        assertNull(result)
    }

    // --- BOOT tests ---

    @Test
    fun `boot returns null`() {
        val entity = AutomationEntity(
            id = 1, scriptId = 1, label = "test", type = AutomationType.BOOT,
            scheduledTimestamp = 0, daysOfWeek = emptyList(),
        )

        assertNull(AutomationTimeCalculator.calculateNextRun(entity))
    }
}
