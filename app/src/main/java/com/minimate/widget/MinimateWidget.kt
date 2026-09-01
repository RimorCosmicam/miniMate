package com.minimate.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import android.widget.RemoteViews
import com.minimate.MinimateApp
import com.minimate.R
import com.minimate.touchpad.model.TouchpadPreferences
import com.minimate.touchpad.model.TouchpadSettings

private const val ACTION_TOGGLE = "com.minimate.widget.TOGGLE"
private const val EXTRA_WHICH = "which"

private const val SPEAKER = "speaker"
private const val MICROPHONE = "microphone"
private const val CAMERA = "camera"

/** Lit when that part of the app is running, dim when it is not — the rule every control follows. */
private const val ON_ALPHA = 255
private const val OFF_ALPHA = 85

/**
 * The three things the app does, on the home screen.
 *
 * A tap has to work whether or not the app happens to be running, so it writes through the same
 * stored settings the app reads on launch and then hands the change to the running session if
 * there is one. Either way the widget and the app agree afterwards about what is on, regardless of
 * which of them was asked.
 */
abstract class MinimateWidget(private val layout: Int) : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val settings = TouchpadPreferences(context).loadSettings()
        ids.forEach { id -> manager.updateAppWidget(id, render(context, layout, settings)) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return

        val preferences = TouchpadPreferences(context)
        val current = preferences.loadSettings()
        val updated = when (intent.getStringExtra(EXTRA_WHICH)) {
            SPEAKER -> current.copy(audioOutputEnabled = !current.audioOutputEnabled)
            MICROPHONE -> current.copy(audioMicrophoneEnabled = !current.audioMicrophoneEnabled)
            CAMERA -> current.copy(webcamEnabled = !current.webcamEnabled)
            else -> return
        }
        preferences.saveSettings(updated)
        // Reaches the live session if there is one. The widget is not a reason to start the app.
        (context.applicationContext as? MinimateApp)?.applyWidgetSettings(updated)
        refreshAll(context)
    }
}

/**
 * Three sizes. Separate providers because a widget's size is declared, not chosen — one block, two
 * across, and a square, so it can sit wherever there is room rather than only where it fits.
 */
class MinimateWidgetSmall : MinimateWidget(R.layout.widget_small)
class MinimateWidgetMedium : MinimateWidget(R.layout.widget_medium)
class MinimateWidgetLarge : MinimateWidget(R.layout.widget_large)

/**
 * The name, drawn rather than set.
 *
 * A widget's layout is inflated by the launcher, in the launcher's process, and a font resource
 * referenced from it does not resolve there — the wordmark came out in whatever the system font
 * happened to be, which is to say not in Mont at all. Painting it here and sending the pixels is
 * the only way to be certain the two words arrive in the right typeface, at the right weights.
 */
private fun wordmark(context: Context, heightPx: Int): Bitmap {
    val thin = runCatching { ResourcesCompat.getFont(context, R.font.mont_thin) }.getOrNull()
    val black = runCatching { ResourcesCompat.getFont(context, R.font.mont_black) }.getOrNull()

    val size = heightPx * 0.46f
    fun paint(face: Typeface?) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = face
        textSize = size
        color = android.graphics.Color.WHITE
    }

    val top = paint(thin)
    val bottom = paint(black)
    val width = maxOf(top.measureText("mini"), bottom.measureText("Mate")).toInt() + 2
    // Mont sits high in its box; setting the lines by their own metrics keeps the pair tight
    // rather than leaving the gap the font's default leading would.
    val lineHeight = -top.ascent() + top.descent()
    val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val block = lineHeight * 1.72f
    val start = (heightPx - block) / 2f - top.ascent()
    canvas.drawText("mini", 0f, start, top)
    canvas.drawText("Mate", 0f, start + lineHeight * 0.86f, bottom)
    return bitmap
}

private fun render(context: Context, layout: Int, settings: TouchpadSettings): RemoteViews =
    RemoteViews(context.packageName, layout).apply {
        val density = context.resources.displayMetrics.density
        setImageViewBitmap(R.id.widget_wordmark, wordmark(context, (46 * density).toInt()))

        fun toggle(viewId: Int, which: String, on: Boolean) {
            setInt(viewId, "setImageAlpha", if (on) ON_ALPHA else OFF_ALPHA)
            setOnClickPendingIntent(viewId, togglePendingIntent(context, which))
        }
        toggle(R.id.widget_speaker, SPEAKER, settings.audioOutputEnabled)
        toggle(R.id.widget_mic, MICROPHONE, settings.audioMicrophoneEnabled)
        toggle(R.id.widget_camera, CAMERA, settings.webcamEnabled)
    }

private fun togglePendingIntent(context: Context, which: String): PendingIntent {
    val intent = Intent(context, MinimateWidgetSmall::class.java).apply {
        action = ACTION_TOGGLE
        putExtra(EXTRA_WHICH, which)
        // Distinct data per toggle, or the three collapse into one intent and the last one wins.
        data = Uri.parse("minimate://widget/$which")
    }
    return PendingIntent.getBroadcast(
        context,
        which.hashCode(),
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/** Every face is redrawn together, since they are all showing the same three facts. */
fun refreshAll(context: Context) {
    val manager = AppWidgetManager.getInstance(context)
    val settings = TouchpadPreferences(context).loadSettings()
    listOf(
        MinimateWidgetSmall::class.java to R.layout.widget_small,
        MinimateWidgetMedium::class.java to R.layout.widget_medium,
        MinimateWidgetLarge::class.java to R.layout.widget_large
    ).forEach { (provider, layout) ->
        manager.getAppWidgetIds(ComponentName(context, provider)).forEach { id ->
            manager.updateAppWidget(id, render(context, layout, settings))
        }
    }
}
