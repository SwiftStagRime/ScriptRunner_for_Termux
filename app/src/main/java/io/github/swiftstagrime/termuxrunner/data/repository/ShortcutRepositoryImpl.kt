package io.github.swiftstagrime.termuxrunner.data.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.drawable.Icon
import android.os.Build
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
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
import androidx.core.graphics.toColorInt

/**
 * Handles the creation of home screen shortcuts for quick script execution.
 */
class ShortcutRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShortcutRepository {

    override fun isPinningSupported(): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    override suspend fun createShortcutInfo(script: Script, useThemedIcon: Boolean): ShortcutInfoCompat? =
        withContext(Dispatchers.IO) {
            if (!isPinningSupported()) return@withContext null

            val iconCompat = try {
                val originalBitmap = script.iconPath?.let { path ->
                    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    })
                }

                if (originalBitmap != null) {
                    val finalBitmap = if (useThemedIcon) {
                        createThemedBitmap(originalBitmap, context)
                    } else {
                        createStandardAdaptiveBitmap(originalBitmap)
                    }

                    IconCompat.createWithAdaptiveBitmap(finalBitmap).also {
                        if (!originalBitmap.isRecycled) originalBitmap.recycle()
                    }
                } else {
                    IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground)
                }
            } catch (e: Exception) {
                IconCompat.createWithResource(context, R.drawable.ic_launcher_foreground)
            }

            ShortcutInfoCompat.Builder(context, "script_${script.id}")
                .setShortLabel(script.name)
                .setIcon(iconCompat)
                .setIntent(createRunnerIntent(script))
                .build()
        }

    private fun createStandardAdaptiveBitmap(src: Bitmap): Bitmap {
        val targetSize = 256
        val output = createBitmap(targetSize, targetSize)
        val canvas = Canvas(output)

        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val sourceWidth = src.width.toFloat()
        val sourceHeight = src.height.toFloat()
        val scale = targetSize.toFloat() / sourceWidth.coerceAtMost(sourceHeight)

        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight

        val left = (targetSize - scaledWidth) / 2f
        val top = (targetSize - scaledHeight) / 2f

        val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        canvas.drawBitmap(src, null, targetRect, paint)

        return output
    }

    private fun createRunnerIntent(script: Script): Intent {
        return Intent(context, ScriptRunnerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra("SCRIPT_ID", script.id)
            data = "scriptrunner://run/${script.id}".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    /**
     * This shit works like ass, but I'm out of ideas
     */
    private fun createThemedBitmap(src: Bitmap, context: Context): Bitmap {
        val targetSize = 256
        val output = createBitmap(targetSize, targetSize)
        val canvas = Canvas(output)

        val (_, accentColor) = getThemeColors(context)

        canvas.drawColor(Color.WHITE)

        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

        val sourceWidth = src.width.toFloat()
        val sourceHeight = src.height.toFloat()
        val scale = targetSize.toFloat() / sourceWidth.coerceAtMost(sourceHeight)

        val scaledWidth = scale * sourceWidth
        val scaledHeight = scale * sourceHeight

        val left = (targetSize - scaledWidth) / 2f
        val top = (targetSize - scaledHeight) / 2f

        val targetRect = RectF(left, top, left + scaledWidth, top + scaledHeight)

        val grayscaleMatrix = ColorMatrix()
        grayscaleMatrix.setSaturation(0f)
        paint.colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
        canvas.drawBitmap(src, null, targetRect, paint)

        val colorPaint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        colorPaint.colorFilter = PorterDuffColorFilter(accentColor, PorterDuff.Mode.MULTIPLY)
        colorPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)

        canvas.drawBitmap(src, null, targetRect, colorPaint)

        return output
    }

    private fun getThemeColors(context: Context): Pair<Int, Int> {
        val isDarkMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkMode) {
                Pair(
                    context.getColor(android.R.color.system_neutral1_800),
                    context.getColor(android.R.color.system_accent1_100)
                )
            } else {
                Pair(
                    context.getColor(android.R.color.system_neutral1_100),
                    context.getColor(android.R.color.system_accent1_800)
                )
            }
        } else {
            if (isDarkMode) Pair(Color.DKGRAY, Color.CYAN)
            else Pair(Color.LTGRAY, Color.BLUE)
        }
    }
}
