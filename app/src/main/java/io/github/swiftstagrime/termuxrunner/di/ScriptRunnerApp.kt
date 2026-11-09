package io.github.swiftstagrime.termuxrunner.di

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import io.github.swiftstagrime.termuxrunner.data.local.KeyManager
import javax.inject.Inject

@HiltAndroidApp
class ScriptRunnerApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var automationScheduler: AutomationScheduler

    @Inject
    lateinit var keyManager: KeyManager

    override fun onCreate() {
        super.onCreate()
        keyManager.prefetch()
        automationScheduler.ensureSweepEnqueued()
        automationScheduler.reconcileEventWatcher()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
