package io.github.swiftstagrime.termuxrunner.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.swiftstagrime.termuxrunner.data.local.AppDatabase
import io.github.swiftstagrime.termuxrunner.data.local.MIGRATION_6_7
import io.github.swiftstagrime.termuxrunner.data.local.MIGRATION_7_8
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationChainDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CustomThemeDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptExecutionDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptVersionDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
        keyManagerFactory: KeyManagerFactory,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "script_runner_secure.db",
            ).openHelperFactory(keyManagerFactory)
            .addMigrations(MIGRATION_6_7, MIGRATION_7_8)
            .enableMultiInstanceInvalidation()
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides
    @Singleton
    fun provideScriptDao(db: AppDatabase): ScriptDao = db.scriptDao()

    @Provides
    @Singleton
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    @Singleton
    fun provideAutomationDao(db: AppDatabase): AutomationDao = db.automationDao()

    @Provides
    @Singleton
    fun provideAutomationLogDao(db: AppDatabase): AutomationLogDao = db.automationLogDao()

    @Provides
    @Singleton
    fun provideCustomThemeDao(db: AppDatabase): CustomThemeDao = db.customThemeDao()

    @Provides
    @Singleton
    fun provideScriptExecutionDao(db: AppDatabase): ScriptExecutionDao = db.scriptExecutionDao()

    @Provides
    @Singleton
    fun provideAutomationChainDao(db: AppDatabase): AutomationChainDao = db.automationChainDao()

    @Provides
    @Singleton
    fun provideScriptVersionDao(db: AppDatabase): ScriptVersionDao = db.scriptVersionDao()
}
