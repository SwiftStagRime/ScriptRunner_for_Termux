package io.github.swiftstagrime.termuxrunner

import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.swiftstagrime.termuxrunner.data.local.ImageStorageManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ImageStorageManagerInstrumentedTest {
    private lateinit var manager: ImageStorageManager
    private lateinit var iconDir: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        manager = ImageStorageManager(context)
        iconDir = File(context.filesDir, "script_icons")
        iconDir.deleteRecursively()
    }

    @After
    fun cleanup() {
        iconDir.deleteRecursively()
    }

    private fun createBitmap(
        width: Int,
        height: Int,
    ): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            for (x in 0 until width) {
                for (y in 0 until height) {
                    setPixel(x, y, -65536)
                }
            }
        }

    private fun saveBitmapAsFileUri(bitmap: Bitmap): Uri {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tempFile = File(context.cacheDir, "test_bitmap_${System.currentTimeMillis()}.png")
        java.io.FileOutputStream(tempFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return Uri.fromFile(tempFile)
    }

    @Test
    fun saveImageFromUri_smallBitmap_noScaling() =
        runTest {
            val smallBitmap = createBitmap(128, 64)
            val uri = saveBitmapAsFileUri(smallBitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()
            val savedFile = File(path)
            assertTrue(savedFile.exists())
            assertTrue(path.endsWith(".webp"))
            assertTrue(path.contains("icon_"))

            val decoded = android.graphics.BitmapFactory.decodeFile(path)
            assertNotNull(decoded)
            assertEquals(128, decoded.width)
            assertEquals(64, decoded.height)
            decoded.recycle()
        }

    @Test
    fun saveImageFromUri_largeBitmap_scalesToTargetSize() =
        runTest {
            val largeBitmap = createBitmap(1920, 1080)
            val uri = saveBitmapAsFileUri(largeBitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()

            val decoded = android.graphics.BitmapFactory.decodeFile(path)
            assertNotNull(decoded)
            assertTrue("Width should be <= 256, got ${decoded.width}", decoded.width <= 256)
            assertTrue("Height should be <= 256, got ${decoded.height}", decoded.height <= 256)

            val aspectRatio = decoded.width.toFloat() / decoded.height.toFloat()
            val originalAspectRatio = 1920f / 1080
            assertTrue(
                "Aspect ratio should be preserved: $aspectRatio vs $originalAspectRatio",
                kotlin.math.abs(aspectRatio - originalAspectRatio) < 0.1,
            )
            decoded.recycle()
        }

    @Test
    fun saveImageFromUri_squareBitmap_scalesEvenly() =
        runTest {
            val squareBitmap = createBitmap(512, 512)
            val uri = saveBitmapAsFileUri(squareBitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()

            val decoded = android.graphics.BitmapFactory.decodeFile(path)
            assertNotNull(decoded)
            assertEquals(256, decoded.width)
            assertEquals(256, decoded.height)
            decoded.recycle()
        }

    @Test
    fun saveImageFromUri_tallBitmap_preservesAspectRatio() =
        runTest {
            val tallBitmap = createBitmap(100, 400)
            val uri = saveBitmapAsFileUri(tallBitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()

            val decoded = android.graphics.BitmapFactory.decodeFile(path)
            assertNotNull(decoded)
            assertEquals(64, decoded.width)
            assertEquals(256, decoded.height)
            decoded.recycle()
        }

    @Test
    fun saveImageFromUri_producesUniqueFilenames() =
        runTest {
            val bitmap = createBitmap(100, 100)
            val uri1 = saveBitmapAsFileUri(bitmap)

            val result1 = manager.saveImageFromUri(uri1)
            assertTrue(result1.isSuccess)
            val path1 = result1.getOrThrow()

            val uri2 = saveBitmapAsFileUri(bitmap)
            val result2 = manager.saveImageFromUri(uri2)
            assertTrue(result2.isSuccess)
            val path2 = result2.getOrThrow()

            assertFalse("Filenames should be unique", path1 == path2)
        }

    @Test
    fun saveImageFromUri_createsIconDirectoryIfNeeded() =
        runTest {
            assertTrue(!iconDir.exists())

            val bitmap = createBitmap(50, 50)
            val uri = saveBitmapAsFileUri(bitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            assertTrue(iconDir.exists())
            assertTrue(iconDir.isDirectory)
        }

    @Test
    fun saveImageFromUri_invalidUri_returnsFailure() =
        runTest {
            val invalidUri = Uri.parse("content://nonexistent.provider/nonexistent")

            val result = manager.saveImageFromUri(invalidUri)

            assertFalse(result.isSuccess)
        }

    @Test
    fun saveImageFromUri_exactTargetSize_noScaling() =
        runTest {
            val exactBitmap = createBitmap(256, 256)
            val uri = saveBitmapAsFileUri(exactBitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()

            val decoded = android.graphics.BitmapFactory.decodeFile(path)
            assertNotNull(decoded)
            assertEquals(256, decoded.width)
            assertEquals(256, decoded.height)
            decoded.recycle()
        }

    @Test
    fun saveImageFromUri_oneOverTarget_otherUnder_noScalingOnSmallDimension() =
        runTest {
            val bitmap = createBitmap(300, 100)
            val uri = saveBitmapAsFileUri(bitmap)

            val result = manager.saveImageFromUri(uri)

            assertTrue(result.isSuccess)
            val path = result.getOrThrow()

            val decoded = android.graphics.BitmapFactory.decodeFile(path)
            assertNotNull(decoded)
            assertEquals(256, decoded.width)
            assertEquals(85, decoded.height)
            decoded.recycle()
        }
}
