package io.github.swiftstagrime.termuxrunner.domain.repository

import androidx.core.content.pm.ShortcutInfoCompat
import io.github.swiftstagrime.termuxrunner.domain.model.Script

interface ShortcutRepository {
    fun isPinningSupported(): Boolean

    suspend fun createShortcutInfo(script: Script): ShortcutInfoCompat?
}
