package io.github.swiftstagrime.termuxrunner.ui.features.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import io.github.swiftstagrime.termuxrunner.ui.features.widget.ScriptWidget.Companion.ScriptIdActionKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.fillMaxWidth
import androidx.glance.Button
import androidx.glance.ButtonDefaults
import androidx.glance.ColorFilter
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.text.Text
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import java.io.File
import androidx.core.graphics.createBitmap
import androidx.glance.layout.ContentScale

@Composable
fun ScriptWidgetContent(scripts: List<Script>, appWidgetId: Int) {
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .padding(12.dp)
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Placeholder",
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp),
                modifier = GlanceModifier.defaultWeight()
            )

            val intent = Intent(context, WidgetConfigurationActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                action = AppWidgetManager.ACTION_APPWIDGET_CONFIGURE
            }

            Image(
                provider = ImageProvider(R.drawable.ic_edit),
                contentDescription = "Edit",
                modifier = GlanceModifier
                    .size(24.dp)
                    .clickable(actionStartActivity(intent)),
                colorFilter = ColorFilter.tint(GlanceTheme.colors.onSurface)
            )
        }

        Spacer(modifier = GlanceModifier.height(12.dp))

        if (scripts.isEmpty()) {
            Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No scripts assigned", style = TextStyle(color = GlanceTheme.colors.onSurface))
            }
        } else {
            val row1 = scripts.take(2)
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                row1.forEachIndexed { index, script ->
                    ScriptTile(script, GlanceModifier.defaultWeight())
                    if (index == 0 && row1.size > 1) Spacer(GlanceModifier.width(8.dp))
                }
            }

            if (scripts.size > 2) {
                Spacer(modifier = GlanceModifier.height(8.dp))
                val row2 = scripts.drop(2)
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    row2.forEachIndexed { index, script ->
                        ScriptTile(script, GlanceModifier.defaultWeight())
                        if (index < row2.size - 1) Spacer(GlanceModifier.width(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptTile(script: Script, modifier: GlanceModifier) {
    var useDefaultDrawable = true
    val imageProvider = remember(script.iconPath) {
        val file = script.iconPath?.let { File(it) }
        if (file != null && file.exists()) {
            ImageProvider(BitmapFactory.decodeFile(file.absolutePath))
                .also {
                    useDefaultDrawable = false
                }
        } else {
            ImageProvider(R.drawable.ic_terminal_tile)
        }
    }
    if (!useDefaultDrawable) {
        Image(
            provider = imageProvider,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = modifier.fillMaxHeight()
                .cornerRadius(16.dp)
                .clickable(actionRunCallback<RunScriptAction>(
                    actionParametersOf(ScriptIdActionKey to script.id)
                ))
        )
    } else {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(16.dp)
            .clickable(actionRunCallback<RunScriptAction>(
                actionParametersOf(ScriptIdActionKey to script.id)
            ))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            provider = imageProvider,
            contentDescription = null,
            modifier = GlanceModifier.size(32.dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.onSecondaryContainer)
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = script.name,
            style = TextStyle(
                color = GlanceTheme.colors.onSecondaryContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
    }}
}

