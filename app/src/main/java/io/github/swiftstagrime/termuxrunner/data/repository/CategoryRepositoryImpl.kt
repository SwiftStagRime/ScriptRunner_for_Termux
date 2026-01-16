package io.github.swiftstagrime.termuxrunner.data.repository

import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.toCategoryDomain
import io.github.swiftstagrime.termuxrunner.data.local.entity.toCategoryEntity
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl
    @Inject
    constructor(
        private val categoryDao: CategoryDao,
    ) : CategoryRepository {
        override fun getAllCategories(): Flow<List<Category>> =
            categoryDao.getAllCategories().map { entities ->
                entities.map { it.toCategoryDomain() }
            }

        override suspend fun getCategoryById(id: Int): Category? = categoryDao.getCategoryById(id)?.toCategoryDomain()

        override suspend fun upsertCategory(category: Category): Int =
            if (category.id == 0) {
                categoryDao.insertCategory(category.toCategoryEntity()).toInt()
            } else {
                categoryDao.updateCategory(category.toCategoryEntity())
                category.id
            }

        override suspend fun deleteCategory(category: Category) {
            categoryDao.deleteCategory(category.toCategoryEntity())
        }
    }
