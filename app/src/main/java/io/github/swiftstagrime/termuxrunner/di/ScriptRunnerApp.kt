package io.github.swiftstagrime.termuxrunner.di

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.github.swiftstagrime.termuxrunner.data.automation.AutomationScheduler
import javax.inject.Inject

@HiltAndroidApp
class ScriptRunnerApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var automationScheduler: AutomationScheduler

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
        automationScheduler.ensureSweepEnqueued()
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
