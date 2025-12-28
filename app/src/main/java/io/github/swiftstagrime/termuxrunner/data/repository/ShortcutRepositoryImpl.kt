package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.ShortcutRepository
import io.github.swiftstagrime.termuxrunner.ui.extensions.UiText
import io.github.swiftstagrime.termuxrunner.ui.features.runner.ScriptRunnerActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
/**
 * Handles the creation of home screen shortcuts for quick script execution.
 */
class ShortcutRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : ShortcutRepository {

    override fun isPinningSupported(): Boolean {
        return ShortcutManagerCompat.isRequestPinShortcutSupported(context)
    }

    override suspend fun createShortcutInfo(script: Script): ShortcutInfoCompat? = withContext(
        Dispatchers.IO
    ) {
        if (!isPinningSupported()) return@withContext null

        // Configure intent to trigger the script execution activity via a deep link
        val intent = Intent(context, ScriptRunnerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("SCRIPT_ID", script.id)
            data = "scriptrunner://run/${script.id}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val iconCompat = try {
            if (script.iconPath != null) {
                createAdaptiveIcon(script.iconPath)
            } else {
                IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground)
            }
        } catch (_: Exception) {
            IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground)
        }

        return@withContext ShortcutInfoCompat.Builder(context, "script_${script.id}")
            .setShortLabel(script.name)
            .setLongLabel(script.name)
            .setIcon(iconCompat)
            .setIntent(intent)
            .build()
    }


    private fun createAdaptiveIcon(path: String): IconCompat {
        val options = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
        val originalBitmap = BitmapFactory.decodeFile(path, options)
            ?: throw IllegalArgumentException(
                UiText.StringResource(R.string.error_decode_icon).asString(context)
            )

        // Process the icon to meet Android's adaptive icon requirements (scaling and centering)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val targetSize = 256

            val output = createBitmap(targetSize, targetSize)
            val canvas = Canvas(output)

            val sourceWidth = originalBitmap.width
            val sourceHeight = originalBitmap.height
            val scale = (targetSize.toFloat() / sourceWidth.coerceAtMost(sourceHeight))

            val scaledWidth = scale * sourceWidth
            val scaledHeight = scale * sourceHeight

            val left = (targetSize - scaledWidth) / 2
            val top = (targetSize - scaledHeight) / 2

            val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

            val paint = Paint().apply { isFilterBitmap = true }
            canvas.drawBitmap(originalBitmap, null, targetRect, paint)

            if (originalBitmap != output) {
                originalBitmap.recycle()
            }

            return IconCompat.createWithAdaptiveBitmap(output)
        } else {
            return IconCompat.createWithBitmap(originalBitmap)
        }
    }
}