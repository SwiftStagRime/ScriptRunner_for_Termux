package io.github.swiftstagrime.termuxrunner.domain.model

data class ScriptVersion(
    val id: Int,
    val scriptId: Int,
    val codePages: List<String>,
    val pageNames: List<String>,
    val timestamp: Long,
    val label: String?,
) {
    val code: String
        get() = codePages.joinToString("\n")
}
