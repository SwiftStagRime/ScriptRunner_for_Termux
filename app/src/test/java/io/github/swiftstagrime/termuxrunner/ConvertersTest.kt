package io.github.swiftstagrime.termuxrunner

import io.github.swiftstagrime.termuxrunner.data.local.Converters
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `envMapConverters - correctly handles a valid map`() {
        val originalMap = mapOf("PATH" to "/bin", "HOME" to "/home/user")
        val jsonString = converters.fromEnvMap(originalMap)
        val resultMap = converters.toEnvMap(jsonString)

        assertEquals("{\"PATH\":\"/bin\",\"HOME\":\"/home/user\"}", jsonString)
        assertEquals(originalMap, resultMap)
    }

    @Test
    fun `envMapConverters - correctly handles an empty map`() {
        val originalMap = emptyMap<String, String>()
        val jsonString = converters.fromEnvMap(originalMap)
        val resultMap = converters.toEnvMap(jsonString)

        assertEquals("{}", jsonString)
        assertEquals(originalMap, resultMap)
    }

    @Test
    fun `toEnvMap - returns empty map for blank or invalid input`() {
        assertEquals(emptyMap<String, String>(), converters.toEnvMap(""))
        assertEquals(emptyMap<String, String>(), converters.toEnvMap("   "))
        assertEquals(emptyMap<String, String>(), converters.toEnvMap("not json"))
        assertEquals(emptyMap<String, String>(), converters.toEnvMap("{\"key\":\"value\""))
    }

    @Test
    fun `interactionModeConverters - correctly handles all enum values`() {
        InteractionMode.entries.forEach { mode ->
            val stringValue = converters.fromInteractionMode(mode)
            val resultMode = converters.toInteractionMode(stringValue)
            assertEquals(mode.name, stringValue)
            assertEquals(mode, resultMode)
        }
    }

    @Test
    fun `toInteractionMode - returns NONE for invalid or unknown input`() {
        assertEquals(InteractionMode.NONE, converters.toInteractionMode("INVALID_MODE"))
        assertEquals(InteractionMode.NONE, converters.toInteractionMode(""))
    }

    @Test
    fun `stringListConverters - correctly handles a valid list`() {
        val originalList = listOf("arg1", "arg2 with spaces", "")
        val jsonString = converters.fromStringList(originalList)
        val resultList = converters.toStringList(jsonString)

        assertEquals("[\"arg1\",\"arg2 with spaces\",\"\"]", jsonString)
        assertEquals(originalList, resultList)
    }

    @Test
    fun `stringListConverters - correctly handles an empty list`() {
        val originalList = emptyList<String>()
        val jsonString = converters.fromStringList(originalList)
        val resultList = converters.toStringList(jsonString)

        assertEquals("[]", jsonString)
        assertEquals(originalList, resultList)
    }

    @Test
    fun `toStringList - returns empty list for blank or invalid input`() {
        assertEquals(emptyList<String>(), converters.toStringList(""))
        assertEquals(emptyList<String>(), converters.toStringList("  "))
        assertEquals(emptyList<String>(), converters.toStringList("not json array"))
        assertEquals(emptyList<String>(), converters.toStringList("[\"item1\""))
    }

    @Test
    fun `automationTypeConverters - correctly handles all enum values`() {
        AutomationType.entries.forEach { type ->
            val stringValue = converters.fromAutomationType(type)
            val resultType = converters.toAutomationType(stringValue)
            assertEquals(type.name, stringValue)
            assertEquals(type, resultType)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toAutomationType - throws exception for invalid input`() {
        converters.toAutomationType("INVALID_TYPE")
    }

    @Test
    fun `intListConverters - correctly handles a valid list`() {
        val originalList = listOf(1, 5, -10, 0, 12345)
        val jsonString = converters.fromIntList(originalList)
        val resultList = converters.toIntList(jsonString)

        assertEquals("[1,5,-10,0,12345]", jsonString)
        assertEquals(originalList, resultList)
    }

    @Test
    fun `intListConverters - correctly handles an empty list`() {
        val originalList = emptyList<Int>()
        val jsonString = converters.fromIntList(originalList)
        val resultList = converters.toIntList(jsonString)

        assertEquals("[]", jsonString)
        assertEquals(originalList, resultList)
    }

    @Test
    fun `toIntList - returns empty list for blank or invalid input`() {
        assertEquals(emptyList<Int>(), converters.toIntList(""))
        assertEquals(emptyList<Int>(), converters.toIntList("  "))
        assertEquals(emptyList<Int>(), converters.toIntList("not a json int array"))
        assertEquals(emptyList<Int>(), converters.toIntList("[1,2,3"))
    }
}
