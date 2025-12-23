package io.github.swiftstagrime.termuxrunner.domain.repository

interface ScriptFileRepository {

    fun saveToBridge(fileName: String, code: String): String
}