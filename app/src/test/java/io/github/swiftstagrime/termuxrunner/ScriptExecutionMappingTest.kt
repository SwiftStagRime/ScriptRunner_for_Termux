package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptExecutionEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.toEntity
import io.github.swiftstagrime.termuxrunner.domain.model.ExecutionSource
import io.github.swiftstagrime.termuxrunner.domain.model.ScriptExecution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptExecutionMappingTest {
    private val testEntity =
        ScriptExecutionEntity(
            id = 1L,
            scriptId = 42,
            scriptName = "TestScript",
            timestamp = 1700000000000L,
            exitCode = 0,
            durationMs = 1500L,
            runtimeArgs = "--verbose --debug",
            source = ScriptExecutionEntity.ExecutionSource.AUTOMATION,
            errorMessage = null,
        )

    @Test
    fun `entity toDomain maps all fields correctly`() {
        val domain = testEntity.toDomain()

        assertEquals("id mismatch", 1L, domain.id)
        assertEquals("scriptId mismatch", 42, domain.scriptId)
        assertEquals("scriptName mismatch", "TestScript", domain.scriptName)
        assertEquals("timestamp mismatch", 1700000000000L, domain.timestamp)
        assertEquals("exitCode mismatch", 0, domain.exitCode)
        assertEquals("durationMs mismatch", 1500L, domain.durationMs)
        assertEquals("runtimeArgs mismatch", "--verbose --debug", domain.runtimeArgs)
        assertEquals("source mismatch", ExecutionSource.AUTOMATION, domain.source)
        assertNull("errorMessage should be null", domain.errorMessage)
    }

    @Test
    fun `entity toDomain maps all ExecutionSource values`() {
        val sources = ScriptExecutionEntity.ExecutionSource.entries.toTypedArray()

        for (entitySource in sources) {
            val entity = testEntity.copy(source = entitySource)
            val domain = entity.toDomain()

            assertEquals(
                "Source mapping failed: ${entitySource.name} -> ${domain.source.name}",
                entitySource.name,
                domain.source.name,
            )
        }
    }

    @Test
    fun `domain toEntity maps all fields correctly`() {
        val domain =
            ScriptExecution(
                id = 1L,
                scriptId = 42,
                scriptName = "TestScript",
                timestamp = 1700000000000L,
                exitCode = 1,
                durationMs = 3000L,
                runtimeArgs = "-f test.txt",
                source = ExecutionSource.TILE,
                errorMessage = "Some error occurred",
            )

        val entity = domain.toEntity()

        assertEquals("id mismatch", 1L, entity.id)
        assertEquals("scriptId mismatch", 42, entity.scriptId)
        assertEquals("scriptName mismatch", "TestScript", entity.scriptName)
        assertEquals("timestamp mismatch", 1700000000000L, entity.timestamp)
        assertEquals("exitCode mismatch", 1, entity.exitCode)
        assertEquals("durationMs mismatch", 3000L, entity.durationMs)
        assertEquals("runtimeArgs mismatch", "-f test.txt", entity.runtimeArgs)
        assertEquals("source mismatch", ScriptExecutionEntity.ExecutionSource.TILE, entity.source)
        assertEquals("errorMessage mismatch", "Some error occurred", entity.errorMessage)
    }

    @Test
    fun `domain toEntity maps all ExecutionSource values`() {
        val sources = ExecutionSource.entries.toTypedArray()

        for (domainSource in sources) {
            val domain =
                ScriptExecution(
                    scriptId = 1,
                    scriptName = "Test",
                    timestamp = System.currentTimeMillis(),
                    exitCode = 0,
                    source = domainSource,
                )
            val entity = domain.toEntity()

            assertEquals(
                "Source mapping failed: ${domainSource.name} -> ${entity.source.name}",
                domainSource.name,
                entity.source.name,
            )
        }
    }

    @Test
    fun `roundtrip entity to domain and back preserves all data`() {
        val originalEntity =
            ScriptExecutionEntity(
                id = 5L,
                scriptId = 99,
                scriptName = "RoundTrip",
                timestamp = 1234567890L,
                exitCode = 0,
                durationMs = 100L,
                runtimeArgs = null,
                source = ScriptExecutionEntity.ExecutionSource.WIDGET,
                errorMessage = "test error",
            )

        val domain = originalEntity.toDomain()
        val restoredEntity = domain.toEntity()

        assertEquals(
            "scriptId mismatch after roundtrip",
            originalEntity.scriptId,
            restoredEntity.scriptId,
        )
        assertEquals(
            "scriptName mismatch after roundtrip",
            originalEntity.scriptName,
            restoredEntity.scriptName,
        )
        assertEquals(
            "timestamp mismatch after roundtrip",
            originalEntity.timestamp,
            restoredEntity.timestamp,
        )
        assertEquals(
            "exitCode mismatch after roundtrip",
            originalEntity.exitCode,
            restoredEntity.exitCode,
        )
        assertEquals(
            "durationMs mismatch after roundtrip",
            originalEntity.durationMs,
            restoredEntity.durationMs,
        )
        assertEquals(
            "runtimeArgs mismatch after roundtrip",
            originalEntity.runtimeArgs,
            restoredEntity.runtimeArgs,
        )
        assertEquals(
            "source mismatch after roundtrip",
            originalEntity.source,
            restoredEntity.source,
        )
        assertEquals(
            "errorMessage mismatch after roundtrip",
            originalEntity.errorMessage,
            restoredEntity.errorMessage,
        )
    }

    @Test
    fun `ScriptExecution isSuccess returns true for exitCode 0`() {
        val success =
            ScriptExecution(
                scriptId = 1,
                scriptName = "ok",
                timestamp = System.currentTimeMillis(),
                exitCode = 0,
            )
        assertTrue("Should be successful", success.isSuccess)
    }

    @Test
    fun `ScriptExecution isSuccess returns false for non-zero exitCode`() {
        val failure =
            ScriptExecution(
                scriptId = 1,
                scriptName = "fail",
                timestamp = System.currentTimeMillis(),
                exitCode = 127,
            )
        assertFalse("Should not be successful", failure.isSuccess)
    }

    @Test
    fun `ScriptExecution statusText shows correct format`() {
        val success =
            ScriptExecution(
                scriptId = 1,
                scriptName = "ok",
                timestamp = System.currentTimeMillis(),
                exitCode = 0,
            )
        assertEquals("Expected 'Success'", "Success", success.statusText)

        val failure =
            ScriptExecution(
                scriptId = 1,
                scriptName = "fail",
                timestamp = System.currentTimeMillis(),
                exitCode = 42,
            )
        assertEquals("Expected 'Failed (code 42)'", "Failed (code 42)", failure.statusText)
    }

    @Test
    fun `entity toDomain with null fields`() {
        val entity =
            ScriptExecutionEntity(
                id = 0,
                scriptId = 1,
                scriptName = "Minimal",
                timestamp = System.currentTimeMillis(),
                exitCode = -1,
                durationMs = null,
                runtimeArgs = null,
                source = ScriptExecutionEntity.ExecutionSource.MANUAL,
                errorMessage = null,
            )

        val domain = entity.toDomain()

        assertNull("durationMs should be null", domain.durationMs)
        assertNull("runtimeArgs should be null", domain.runtimeArgs)
        assertNull("errorMessage should be null", domain.errorMessage)
        assertEquals("Default source should be MANUAL", ExecutionSource.MANUAL, domain.source)
    }
}
