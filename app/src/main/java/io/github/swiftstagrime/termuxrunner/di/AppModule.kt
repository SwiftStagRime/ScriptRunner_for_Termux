package io.github.swiftstagrime.termuxrunner.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.ImageStorageManager
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.AutomationLogDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.CategoryDao
import io.github.swiftstagrime.termuxrunner.data.local.dao.ScriptDao
import io.github.swiftstagrime.termuxrunner.data.repository.AutomationLogRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.AutomationRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.CategoryRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.IconRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.MonitoringRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptFileRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.ScriptRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.ShortcutRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxRepositoryImpl
import io.github.swiftstagrime.termuxrunner.data.repository.UserPreferencesRepositoryImpl
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationLogRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.AutomationRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.IconRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.MonitoringRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptFileRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ShortcutRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.TermuxRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideScriptRepository(
        dao: ScriptDao,
        categoryDao: CategoryDao,
        automationDao: AutomationDao,
        @ApplicationContext context: Context
    ): ScriptRepository {
        return ScriptRepositoryImpl(dao, categoryDao, automationDao, context)
    }

    @Provides
    @Singleton
    fun provideTermuxRepository(@ApplicationContext context: Context): TermuxRepository {
        return TermuxRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideShortcutRepository(@ApplicationContext context: Context): ShortcutRepository {
        return ShortcutRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideScriptFileRepository(@ApplicationContext context: Context): ScriptFileRepository {
        return ScriptFileRepositoryImpl(context)
    }


    @Provides
    @Singleton
    fun provideImageStorageManager(@ApplicationContext context: Context): ImageStorageManager {
        return ImageStorageManager(context)
    }

    @Provides
    @Singleton
    fun provideIconRepository(imageStorageManager: ImageStorageManager): IconRepository {
        return IconRepositoryImpl(imageStorageManager)
    }

    @Provides
    @Singleton
    fun provideMonitoringRepository(
        @ApplicationContext context: Context
    ): MonitoringRepository {
        return MonitoringRepositoryImpl(context)
    }

    @Provides
    @Singleton
    fun provideAutomationRepository(
        automationDao: AutomationDao,
        scheduler: AutomationScheduler
    ): AutomationRepository {
        return AutomationRepositoryImpl(
            automationDao,
            scheduler = scheduler
        )
    }

    @Provides
    @Singleton
    fun provideAutomationScheduler(
        @ApplicationContext context: Context
    ): AutomationScheduler {
        return AutomationScheduler(context)
    }


    @Provides
    @Singleton
    fun provideCategoryRepository(
        categoryDao: CategoryDao
    ): CategoryRepository {
        return CategoryRepositoryImpl(categoryDao)
    }

    @Provides
    @Singleton
    fun provideAutomationLogRepository(
        automationLogDao: AutomationLogDao
    ): AutomationLogRepository {
        return AutomationLogRepositoryImpl(automationLogDao)
    }
}