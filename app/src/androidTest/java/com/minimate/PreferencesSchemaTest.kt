package com.minimate

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.minimate.touchpad.model.ClockStyle
import com.minimate.touchpad.model.TouchpadPreferences
import com.minimate.touchpad.model.TouchpadSettings
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A backup restored onto a fresh install carries settings written by whatever the app looked like
 * when the backup was taken. The clock is the visible casualty: an old position puts the pill at
 * the top of the cover screen, over the camera, and an old style draws it as glass instead of Mont.
 */
@RunWith(AndroidJUnit4::class)
class PreferencesSchemaTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val store = context.getSharedPreferences("minimate_touchpad_prefs", 0)

    @Before
    fun clear() = store.edit().clear().commit().let { }

    private fun writeRaw(json: JSONObject) =
        store.edit().putString("saved_touchpad_settings_json", json.toString()).commit()

    @Test
    fun settingsFromBeforeTheStampLoseTheirClockLayout() {
        val fresh = TouchpadSettings()
        writeRaw(
            JSONObject()
                .put("clockPositionX", 0.50)
                .put("clockPositionY", 0.09)
                .put("clockScale", 1.0)
                .put("clockStyle", ClockStyle.MINIMAL_PILL.name)
                // Something the user actually chose, which must survive.
                .put("scrollSpeed", 2.5)
        )

        val loaded = TouchpadPreferences(context).loadSettings()

        assertEquals(fresh.clockPositionX, loaded.clockPositionX, 0.0001f)
        assertEquals(fresh.clockPositionY, loaded.clockPositionY, 0.0001f)
        assertEquals(fresh.clockScale, loaded.clockScale, 0.0001f)
        assertEquals(ClockStyle.MONT, loaded.clockStyle)
        assertEquals(2.5f, loaded.scrollSpeed, 0.0001f)
    }

    @Test
    fun aClockTheUserMovedHimselfStaysWhereHePutIt() {
        val prefs = TouchpadPreferences(context)
        prefs.saveSettings(
            TouchpadSettings(
                clockPositionX = 0.71f,
                clockPositionY = 0.33f,
                clockScale = 1.4f,
                clockStyle = ClockStyle.MONOSPACE
            )
        )

        val loaded = prefs.loadSettings()

        assertEquals(0.71f, loaded.clockPositionX, 0.0001f)
        assertEquals(0.33f, loaded.clockPositionY, 0.0001f)
        assertEquals(1.4f, loaded.clockScale, 0.0001f)
        assertEquals(ClockStyle.MONOSPACE, loaded.clockStyle)
    }
}
