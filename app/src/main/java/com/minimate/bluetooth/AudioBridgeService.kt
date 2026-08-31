package com.minimate.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.minimate.MainActivity
import com.minimate.MinimateApp
import com.minimate.R
import com.minimate.touchpad.model.TouchpadPreferences

class AudioBridgeService : Service() {
    companion object {
        const val CHANNEL_ID = "minimate_audio_bridge"
        const val NOTIFICATION_ID = 2408
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MiniMate Audio")
            .setContentText("Audio bridge ready with the screen off")
            .setContentIntent(openApp)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
        val microphoneAllowed = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val foregroundTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                if (microphoneAllowed) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        } else 0
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, foregroundTypes)

        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MiniMate:AudioBridge")
            .apply { setReferenceCounted(false); acquire() }
        wifiLock = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
            .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MiniMate:LosslessAudio")
            .apply { setReferenceCounted(false); acquire() }

        val settings = TouchpadPreferences(this).loadSettings()
        (application as MinimateApp).audioBridge.apply {
            configure(
                settings.audioOutputEnabled,
                settings.audioMicrophoneEnabled,
                settings.audioOutputVolume,
                settings.audioOutputDeviceKey,
                settings.audioDeviceEqProfiles,
                settings.audioMicrophoneGain,
                settings.audioMicrophonePreset,
                settings.audioSuperhumanBands
            )
            start()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        (application as MinimateApp).audioBridge.close()
        if (wifiLock?.isHeld == true) wifiLock?.release()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Audio bridge", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps MiniMate lossless audio and microphone active with the display off"
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}
