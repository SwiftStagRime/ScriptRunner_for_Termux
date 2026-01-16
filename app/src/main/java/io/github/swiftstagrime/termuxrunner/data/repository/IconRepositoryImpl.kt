package io.github.swiftstagrime.termuxrunner.data.repository

import androidx.core.net.toUri
import io.github.swiftstagrime.termuxrunner.data.local.ImageStorageManager
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import javax.inject.Inject

/**
 * Simple repo for better testability, saves icon and returns path
 */
class IconRepositoryImpl
    @Inject
    constructor(
        private val imageStorageManager: ImageStorageManager,
    ) : IconRepository {
        override suspend fun saveIcon(uriStr: String): String? {
            val uri = uriStr.toUri()
            val result = imageStorageManager.saveImageFromUri(uri)
            return result.getOrNull()
        }
    }
