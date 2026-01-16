package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.local.Converters
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `fromEnvMap serializes map to JSON string`() {
        val input = mapOf("API_KEY" to "12345", "MODE" to "dark")
        val result = converters.fromEnvMap(input)

        assert(result.contains("\"API_KEY\":\"12345\""))
        assert(result.contains("\"MODE\":\"dark\""))
        assert(result.startsWith("{"))
        assert(result.endsWith("}"))
    }

    @Test
    fun `toEnvMap deserializes JSON string to map`() {
        val json = """{"HOST":"localhost", "PORT":"8080"}"""
        val result = converters.toEnvMap(json)

        assertEquals(2, result.size)
        assertEquals("localhost", result["HOST"])
        assertEquals("8080", result["PORT"])
    }

    @Test
    fun `toEnvMap handles empty or invalid input gracefully`() {
        val emptyResult = converters.toEnvMap("")
        assertEquals(0, emptyResult.size)

        val badResult = converters.toEnvMap("{ bad json : ")
        assertEquals(0, badResult.size)
    }
}
