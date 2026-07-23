package io.github.swiftstagrime.termuxrunner.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "script_versions",
    foreignKeys = [
        ForeignKey(
            entity = ScriptEntity::class,
            parentColumns = ["id"],
            childColumns = ["scriptId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["scriptId"])],
)
data class ScriptVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val scriptId: Int,
    @ColumnInfo(name = "codePages") val codePages: List<String>,
    @ColumnInfo(name = "page_names", defaultValue = "") val pageNames: List<String> = emptyList(),
    val timestamp: Long,
    val label: String? = null,
)
