package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.swiftstagrime.termuxrunner.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "0")
    val orderIndex: Int = 0,
)

fun CategoryEntity.toCategoryDomain() = Category(id, name, orderIndex)

fun Category.toCategoryEntity() = CategoryEntity(id, name, orderIndex)
