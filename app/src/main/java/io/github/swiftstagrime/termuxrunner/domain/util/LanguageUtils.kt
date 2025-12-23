package io.github.swiftstagrime.termuxrunner.domain.util

object LanguageUtils {
    fun getCommentSymbol(interpreter: String): String {
        return when (interpreter.trim().lowercase()) {
            "python", "python3", "ruby", "perl" -> "#"
            "node", "nodejs", "js", "javascript" -> "//"
            "lua" -> "--"
            else -> "#"
        }
    }
}