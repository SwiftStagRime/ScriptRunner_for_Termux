package io.github.swiftstagrime.termuxrunner.data.local

import androidx.room.TypeConverter
import io.github.swiftstagrime.termuxrunner.domain.model.AutomationType
import io.github.swiftstagrime.termuxrunner.domain.model.InteractionMode
import kotlinx.serialization.json.Json

class Converters {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @TypeConverter
    fun fromEnvMap(map: Map<String, String>): String {
        return json.encodeToString(map)
    }

    @TypeConverter
    fun toEnvMap(data: String): Map<String, String> {
        if (data.isBlank()) return emptyMap()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    @TypeConverter
    fun fromInteractionMode(mode: InteractionMode): String {
        return mode.name
    }

    @TypeConverter
    fun toInteractionMode(data: String): InteractionMode {
        return try {
            InteractionMode.valueOf(data)
        } catch (e: Exception) {
            InteractionMode.NONE
        }
    }

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        if (data.isBlank()) return emptyList()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    @TypeConverter
    fun fromAutomationType(value: AutomationType) = value.name

    @TypeConverter
    fun toAutomationType(value: String) = AutomationType.valueOf(value)

    @TypeConverter
    fun fromIntList(list: List<Int>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toIntList(data: String): List<Int> {
        if (data.isBlank()) return emptyList()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}