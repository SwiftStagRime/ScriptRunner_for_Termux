package io.github.swiftstagrime.termuxrunner.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationChainDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CustomThemeDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptExecutionDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptVersionDao
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationChainEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.AutomationLogEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CategoryEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.CustomThemeEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptExecutionEntity
import io.github.swiftstagrime.termuxrunner.data.local.entity.ScriptVersionEntity
import org.json.JSONArray

@Database(
    entities = [
        ScriptEntity::class,
        CategoryEntity::class,
        AutomationEntity::class,
        AutomationLogEntity::class,
        CustomThemeEntity::class,
        ScriptExecutionEntity::class,
        AutomationChainEntity::class,
        ScriptVersionEntity::class,
    ],
    version = 8,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(
            from = 2,
            to = 3,
        ),
        AutoMigration(from = 3, to = 4), AutoMigration(from = 4, to = 5), AutoMigration(
            from = 5,
            to = 6,
        ),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scriptDao(): ScriptDao

    abstract fun categoryDao(): CategoryDao

    abstract fun automationDao(): AutomationDao

    abstract fun automationLogDao(): AutomationLogDao

    abstract fun customThemeDao(): CustomThemeDao

    abstract fun scriptExecutionDao(): ScriptExecutionDao

    abstract fun automationChainDao(): AutomationChainDao

    abstract fun scriptVersionDao(): ScriptVersionDao
}

val MIGRATION_6_7: Migration =
    object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
            CREATE TABLE scripts_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                codePages TEXT NOT NULL,
                page_names TEXT NOT NULL DEFAULT '',
                interpreter TEXT NOT NULL,
                fileExtension TEXT NOT NULL,
                commandPrefix TEXT NOT NULL,
                runInBackground INTEGER NOT NULL,
                openNewSession INTEGER NOT NULL,
                executionParams TEXT NOT NULL,
                iconPath TEXT,
                envVars TEXT NOT NULL,
                keepSessionOpen INTEGER NOT NULL,
                useHeartbeat INTEGER NOT NULL DEFAULT 0,
                heartbeatTimeout INTEGER NOT NULL DEFAULT 30000,
                heartbeatInterval INTEGER NOT NULL DEFAULT 10000,
                categoryId INTEGER DEFAULT NULL,
                orderIndex INTEGER NOT NULL DEFAULT 0,
                notifyOnResult INTEGER NOT NULL DEFAULT 0,
                interactionMode TEXT NOT NULL DEFAULT 'NONE',
                argumentPresets TEXT NOT NULL DEFAULT '',
                prefixPresets TEXT NOT NULL DEFAULT '',
                envVarPresets TEXT NOT NULL DEFAULT '',
                adbCode TEXT DEFAULT NULL
            )
        """,
            )

            val cursor = database.query("SELECT * FROM scripts")

            cursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val values = ContentValues()

                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val oldCode = cursor.getString(cursor.getColumnIndexOrThrow("code"))

                    val codePagesJson = JSONArray().apply { put(oldCode) }.toString()

                    values.put("id", id)
                    values.put("name", cursor.getString(cursor.getColumnIndexOrThrow("name")))
                    values.put("codePages", codePagesJson)
                    values.put("page_names", "[]")
                    values.put(
                        "interpreter",
                        cursor.getString(cursor.getColumnIndexOrThrow("interpreter")),
                    )
                    values.put(
                        "fileExtension",
                        cursor.getString(cursor.getColumnIndexOrThrow("fileExtension")),
                    )
                    values.put(
                        "commandPrefix",
                        cursor.getString(cursor.getColumnIndexOrThrow("commandPrefix")),
                    )
                    values.put(
                        "runInBackground",
                        cursor.getInt(cursor.getColumnIndexOrThrow("runInBackground")),
                    )
                    values.put(
                        "openNewSession",
                        cursor.getInt(cursor.getColumnIndexOrThrow("openNewSession")),
                    )
                    values.put(
                        "executionParams",
                        cursor.getString(cursor.getColumnIndexOrThrow("executionParams")),
                    )
                    values.put(
                        "iconPath",
                        cursor.getString(cursor.getColumnIndexOrThrow("iconPath")),
                    )
                    values.put("envVars", cursor.getString(cursor.getColumnIndexOrThrow("envVars")))
                    values.put(
                        "keepSessionOpen",
                        cursor.getInt(cursor.getColumnIndexOrThrow("keepSessionOpen")),
                    )
                    values.put(
                        "useHeartbeat",
                        cursor.getInt(cursor.getColumnIndexOrThrow("useHeartbeat")),
                    )
                    values.put(
                        "heartbeatTimeout",
                        cursor.getInt(cursor.getColumnIndexOrThrow("heartbeatTimeout")),
                    )
                    values.put(
                        "heartbeatInterval",
                        cursor.getInt(cursor.getColumnIndexOrThrow("heartbeatInterval")),
                    )

                    val catIdx = cursor.getColumnIndexOrThrow("categoryId")
                    if (cursor.isNull(catIdx)) {
                        values.putNull("categoryId")
                    } else {
                        values.put("categoryId", cursor.getLong(catIdx))
                    }

                    values.put(
                        "orderIndex",
                        cursor.getInt(cursor.getColumnIndexOrThrow("orderIndex")),
                    )
                    values.put(
                        "notifyOnResult",
                        cursor.getInt(cursor.getColumnIndexOrThrow("notifyOnResult")),
                    )
                    values.put(
                        "interactionMode",
                        cursor.getString(cursor.getColumnIndexOrThrow("interactionMode")),
                    )
                    values.put(
                        "argumentPresets",
                        cursor.getString(cursor.getColumnIndexOrThrow("argumentPresets")),
                    )
                    values.put(
                        "prefixPresets",
                        cursor.getString(cursor.getColumnIndexOrThrow("prefixPresets")),
                    )
                    values.put(
                        "envVarPresets",
                        cursor.getString(cursor.getColumnIndexOrThrow("envVarPresets")),
                    )
                    values.put("adbCode", cursor.getString(cursor.getColumnIndexOrThrow("adbCode")))

                    database.insert("scripts_new", SQLiteDatabase.CONFLICT_REPLACE, values)
                }
            }

            database.execSQL("DROP TABLE scripts")
            database.execSQL("ALTER TABLE scripts_new RENAME TO scripts")
        }
    }

val MIGRATION_7_8: Migration =
    object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
            CREATE TABLE script_executions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scriptId INTEGER NOT NULL,
                scriptName TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                exitCode INTEGER NOT NULL,
                durationMs INTEGER DEFAULT NULL,
                runtimeArgs TEXT DEFAULT NULL,
                source TEXT NOT NULL,
                errorMessage TEXT DEFAULT NULL
            )
        """,
            )
            database.execSQL("CREATE INDEX index_script_executions_scriptId ON script_executions(scriptId)")
            database.execSQL("CREATE INDEX index_script_executions_timestamp ON script_executions(timestamp)")

            // New columns on automations for advanced scheduling and event triggers
            database.execSQL("ALTER TABLE automations ADD COLUMN scheduledDayOfMonth INTEGER DEFAULT NULL")
            database.execSQL("ALTER TABLE automations ADD COLUMN windowStartHour INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE automations ADD COLUMN windowStartMinute INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE automations ADD COLUMN windowEndHour INTEGER NOT NULL DEFAULT 23")
            database.execSQL("ALTER TABLE automations ADD COLUMN windowEndMinute INTEGER NOT NULL DEFAULT 59")
            database.execSQL("ALTER TABLE automations ADD COLUMN randomDelayMinMillis INTEGER DEFAULT NULL")
            database.execSQL("ALTER TABLE automations ADD COLUMN randomDelayMaxMillis INTEGER DEFAULT NULL")
            database.execSQL("ALTER TABLE automations ADD COLUMN automationCode TEXT DEFAULT NULL")

            // New column on scripts for notification actions
            database.execSQL("ALTER TABLE scripts ADD COLUMN notificationActions TEXT NOT NULL DEFAULT ''")

            // New column on scripts for foreground session behavior
            database.execSQL(
                "ALTER TABLE scripts ADD COLUMN foregroundSessionBehavior TEXT NOT NULL DEFAULT 'KEEP_OPEN'",
            )

            // New column on scripts for reusing an existing session named after the script
            database.execSQL("ALTER TABLE scripts ADD COLUMN reuseSession INTEGER NOT NULL DEFAULT 0")

            // New table for automation chains
            database.execSQL(
                """
            CREATE TABLE automation_chains (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                triggerAutomationId INTEGER NOT NULL,
                steps TEXT NOT NULL,
                FOREIGN KEY(triggerAutomationId) REFERENCES automations(id) ON DELETE CASCADE
            )
        """,
            )
            database.execSQL("CREATE INDEX index_automation_chains_trigger_id ON automation_chains(triggerAutomationId)")

            // New table for script code versioning
            database.execSQL(
                """
            CREATE TABLE script_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scriptId INTEGER NOT NULL,
                codePages TEXT NOT NULL,
                page_names TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL,
                label TEXT DEFAULT NULL,
                FOREIGN KEY(scriptId) REFERENCES scripts(id) ON DELETE CASCADE
            )
        """,
            )
            database.execSQL("CREATE INDEX index_script_versions_scriptId ON script_versions(scriptId)")
        }
    }
