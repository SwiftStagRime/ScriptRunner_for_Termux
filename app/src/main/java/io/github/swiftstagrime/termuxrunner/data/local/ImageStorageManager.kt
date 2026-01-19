package io.github.swiftstagrime.termuxrunner.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.local.ImageStorageManager.Companion.TARGET_SIZE_PX
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

/**
 * Manages the processing and internal storage of script icons.
 */
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Processes a selected image: decodes, resizes to fit [TARGET_SIZE_PX],
     * and saves it as a WebP file. Returns the absolute path.
     */
    suspend fun saveImageFromUri(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val directory = File(context.filesDir, ICON_DIR).apply {
                if (!exists()) mkdirs()
            }

            val fileName = "icon_${UUID.randomUUID()}.webp"
            val destFile = File(directory, fileName)

            val bitmap = decodeBitmap(uri) ?: return@withContext Result.failure(
                Exception("Failed to decode bitmap")
            )

            val scaledBitmap = scaleBitmapIfNeeded(bitmap)

            saveBitmapToFile(scaledBitmap, destFile)

            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }
            scaledBitmap.recycle()

            Result.success(destFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeBitmap(uri: Uri): Bitmap? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }
        }
    }

    private fun scaleBitmapIfNeeded(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= TARGET_SIZE_PX && bitmap.height <= TARGET_SIZE_PX) {
            return bitmap
        }

        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val width: Int
        val height: Int

        if (aspectRatio > 1) {
            width = TARGET_SIZE_PX
            height = (TARGET_SIZE_PX / aspectRatio).toInt()
        } else {
            width = (TARGET_SIZE_PX * aspectRatio).toInt()
            height = TARGET_SIZE_PX
        }

        return bitmap.scale(width, height)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use { out ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            bitmap.compress(format, 50, out)
        }
    }

    private companion object {
        const val TARGET_SIZE_PX = 256
        const val ICON_DIR = "script_icons"
    }

}
