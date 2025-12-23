package io.github.swiftstagrime.termuxrunner.domain.model

import kotlinx.serialization.Serializable

@Serializable
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
    val openNewSession: Boolean = true,   // If true, opens Termux window
    val keepSessionOpen: Boolean = true,  // If true, adds a hack to keep the screen open, don't really rely on it
)