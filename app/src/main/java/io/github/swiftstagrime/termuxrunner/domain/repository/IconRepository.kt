package io.github.swiftstagrime.termuxrunner.domain.repository

interface IconRepository {
    suspend fun saveIcon(uriStr: String): String?
}