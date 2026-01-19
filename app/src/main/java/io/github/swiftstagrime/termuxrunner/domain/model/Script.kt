package io.github.swiftstagrime.termuxrunner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/*
    I have no idea what I marked this as serializable for, probably json - TODO: remove after testing
 */
@Serializable
@Parcelize
enum class InteractionMode : Parcelable {
    NONE,
    TEXT_INPUT,
    MULTI_CHOICE,
}

@Serializable
@Parcelize
data class Script(
    val id: Int = 0,
    val name: String,
    val code: String,
    val interpreter: String = "bash",
    val fileExtension: String = "sh",
    val commandPrefix: String = "",
    val executionParams: String = "",
    val envVars: Map<String, String> = emptyMap(),
    val iconPath: String? = null,
    val runInBackground: Boolean = false, // If true, shows notification only
    val openNewSession: Boolean = true, // If true, opens Termux window
    val keepSessionOpen: Boolean = true, // If true, adds a hack to keep the screen open, don't really rely on it
    val useHeartbeat: Boolean = false, // Experimental hack to monitor script execution
    val heartbeatTimeout: Long = 30000,
    val heartbeatInterval: Long = 10000,
    val categoryId: Int? = null,
    val orderIndex: Int = 0,
    val notifyOnResult: Boolean = false,
    val interactionMode: InteractionMode = InteractionMode.NONE,
    val argumentPresets: List<String> = emptyList(),
    val prefixPresets: List<String> = emptyList(),
    val envVarPresets: List<String> = emptyList(),
) : Parcelable
