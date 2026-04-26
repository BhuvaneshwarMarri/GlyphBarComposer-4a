package com.smaarig.glyphbarcomposer.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smaarig.glyphbarcomposer.R
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.ui.MainActivity
import com.smaarig.glyphbarcomposer.utils.EnhancedGestureManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class BatteryService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var glyphController: GlyphController
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var gestureManager: EnhancedGestureManager

    companion object {
        private const val CHANNEL_ID = "battery_sync_channel"
        private const val NOTIFICATION_ID = 1001
        
        var isRunning = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        glyphController = GlyphController.getInstance(this)
        batteryMonitor = BatteryMonitor(this)
        gestureManager = EnhancedGestureManager(this) {
            glyphController.showBatteryPeek()
        }
        gestureManager.start()
        
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        
        // Monitor battery and update glyphs
        serviceScope.launch {
            combine(
                batteryMonitor.isCharging,
                batteryMonitor.batteryLevel
            ) { charging, level -> charging to level }
                .collect { (charging, level) ->
                    glyphController.updateBatteryProgress(level, charging)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        batteryMonitor.unregister()
        gestureManager.stop()
        glyphController.updateBatteryProgress(0, false) // Turn off battery lights
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Battery Glyph Sync")
            .setContentText("Representing battery percentage on Glyph lights")
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID, "Battery Sync Service Channel",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
