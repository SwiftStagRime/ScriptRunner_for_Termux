package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.local.dao.CustomThemeDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.toDomain
import io.github.swiftstagrime.termuxrunner.domain.model.CustomTheme
import io.github.swiftstagrime.termuxrunner.domain.model.toEntity
import io.github.swiftstagrime.termuxrunner.domain.repository.CustomThemeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CustomThemeRepositoryImpl
    @Inject
    constructor(
        private val themeDao: CustomThemeDao,
    ) : CustomThemeRepository {
        override fun getAllCustomThemes(): Flow<List<CustomTheme>> = themeDao.getAllThemes().map { list -> list.map { it.toDomain() } }

        override suspend fun getThemeById(id: Int): CustomTheme? = themeDao.getThemeById(id)?.toDomain()

        override suspend fun saveTheme(theme: CustomTheme): Long = themeDao.insertTheme(theme.toEntity())

        override fun getThemeByIdFlow(id: Int): Flow<CustomTheme?> =
            themeDao.getThemeByIdFlow(id).map { entity ->
                entity?.toDomain()
            }

        override suspend fun deleteTheme(theme: CustomTheme) {
            themeDao.deleteTheme(theme.toEntity())
        }
    }
