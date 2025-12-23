package io.github.swiftstagrime.termuxrunner.data.local

import androidx.room.TypeConverter
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
}