package io.github.swiftstagrime.termuxrunner.di

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import androidx.work.Configuration
import dagger.hilt.android.testing.CustomTestApplication

class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?,
    ): Application {
        val testApp = MyHiltTestApp_Application::class.java.name
        return super.newApplication(cl, testApp, context)
    }
}

open class TestBaseApplication :
    Application(),
    Configuration.Provider {
    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("sqlcipher")
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()
}

@CustomTestApplication(TestBaseApplication::class)
interface MyHiltTestApp
