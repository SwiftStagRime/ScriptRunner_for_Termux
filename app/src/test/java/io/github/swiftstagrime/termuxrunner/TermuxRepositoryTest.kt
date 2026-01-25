package io.github.swiftstagrime.termuxrunner

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageInfo
import android.os.PowerManager
import androidx.test.core.app.ApplicationProvider
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxBackgroundRestrictionException
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxNotInstalledException
import io.github.swiftstagrime.termuxrunner.data.repository.TermuxRepositoryImpl
import io.mockk.every
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [33])
class TermuxRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var repository: TermuxRepositoryImpl
    private lateinit var shadowPackageManager: ShadowPackageManager

    @Before
    fun setup() {
        repository = TermuxRepositoryImpl(context)
        shadowPackageManager = shadowOf(context.packageManager)
    }

    @Test
    fun `isTermuxInstalled returns true when package exists`() {
        val packageInfo = PackageInfo().apply { packageName = TermuxRepositoryImpl.TERMUX_PACKAGE }
        shadowPackageManager.installPackage(packageInfo)

        assertTrue(repository.isTermuxInstalled())
    }

    @Test
    fun `isTermuxInstalled returns false when package missing`() {
        assertFalse(repository.isTermuxInstalled())
    }

    @Test
    fun `isPermissionGranted returns true when granted`() {
        shadowOf(context as Application).grantPermissions(TermuxRepositoryImpl.PERMISSION_RUN_COMMAND)
        assertTrue(repository.isPermissionGranted())
    }

    @Test
    fun `runCommand throws NotInstalledException when Termux missing`() {
        assertThrows(TermuxNotInstalledException::class.java) {
            repository.runCommand("ls", true, "0", 1, "Test", false, null)
        }
    }

    @Test
    fun `runCommand starts service with correct intent extras`() {
        val packageInfo = PackageInfo().apply { packageName = TermuxRepositoryImpl.TERMUX_PACKAGE }
        shadowPackageManager.installPackage(packageInfo)
        shadowOf(context as Application).grantPermissions(TermuxRepositoryImpl.PERMISSION_RUN_COMMAND)

        repository.runCommand(
            command = "echo 'hello'",
            runInBackground = true,
            sessionAction = "0",
            scriptId = 123,
            scriptName = "MyScript",
            notifyOnResult = true,
            automationId = 456,
        )

        val nextServiceIntent = shadowOf(context).nextStartedService
        assertNotNull(nextServiceIntent)
        assertEquals(TermuxRepositoryImpl.TERMUX_PACKAGE, nextServiceIntent.component?.packageName)
        assertEquals("com.termux.app.RunCommandService", nextServiceIntent.component?.className)

        val path = nextServiceIntent.getStringExtra(TermuxRepositoryImpl.EXTRA_COMMAND_PATH)
        assertTrue(path!!.endsWith("/com.termux/files/usr/bin/bash"))

        val args = nextServiceIntent.getStringArrayExtra(TermuxRepositoryImpl.EXTRA_ARGUMENTS)
        assertArrayEquals(arrayOf("-c", "echo 'hello'"), args)

        val pendingIntent = nextServiceIntent.getParcelableExtra<PendingIntent>(TermuxRepositoryImpl.EXTRA_PENDING_INTENT)
        assertNotNull(pendingIntent)

        val shadowPendingIntent = shadowOf(pendingIntent)
        val resultIntent = shadowPendingIntent.savedIntent
        assertEquals("io.github.swiftstagrime.SCRIPT_RESULT", resultIntent.action)
        assertEquals(123, resultIntent.getIntExtra("script_id", -1))
        assertEquals(456, resultIntent.getIntExtra("automation_id", -1))
    }

    @Test
    fun `runCommand handles ForegroundServiceStartNotAllowedException`() {
        val packageInfo = PackageInfo().apply { packageName = TermuxRepositoryImpl.TERMUX_PACKAGE }
        shadowPackageManager.installPackage(packageInfo)
        shadowOf(context as Application).grantPermissions(TermuxRepositoryImpl.PERMISSION_RUN_COMMAND)

        val spyContext = spyk(context)
        val repoWithSpy = TermuxRepositoryImpl(spyContext)

        every {
            spyContext.startForegroundService(any())
        } throws RuntimeException("ForegroundServiceStartNotAllowedException")

        assertThrows(TermuxBackgroundRestrictionException::class.java) {
            repoWithSpy.runCommand("ls", true, "0", 1, "Test", false, null)
        }
    }

    @Test
    fun `isTermuxBatteryOptimized returns correct state`() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val shadowPower = shadowOf(powerManager)

        shadowPower.setIgnoringBatteryOptimizations(TermuxRepositoryImpl.TERMUX_PACKAGE, true)
        assertTrue(repository.isTermuxBatteryOptimized())

        shadowPower.setIgnoringBatteryOptimizations(TermuxRepositoryImpl.TERMUX_PACKAGE, false)
        assertFalse(repository.isTermuxBatteryOptimized())
    }
}
