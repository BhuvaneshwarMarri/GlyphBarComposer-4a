package com.smaarig.glyphbarcomposer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.R
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.ui.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GlyphPlaybackService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var playbackJob: Job? = null
    private lateinit var glyphController: GlyphController

    override fun onCreate() {
        super.onCreate()
        glyphController = GlyphController.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val playlistId = intent?.getLongExtra(EXTRA_PLAYLIST_ID, -1L) ?: -1L
        Log.d("GlyphPlaybackService", "onStartCommand: action=$action, playlistId=$playlistId")

        when (action) {
            ACTION_START -> {
                if (playlistId != -1L) {
                    startPlayback(playlistId)
                }
            }

            ACTION_STOP -> {
                stopPlayback()
            }
        }

        return START_NOT_STICKY
    }

    private fun startPlayback(playlistId: Long) {
        playbackJob?.cancel()
        playbackJob = serviceScope.launch {
            val app = application as GlyphApplication
            val playlist = app.repository.getPlaylistWithSteps(playlistId) ?: return@launch
            Log.d("GlyphPlaybackService", "Starting playback for playlist: ${playlist.playlist.name}")
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    createNotification(playlist.playlist.name),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification(playlist.playlist.name))
            }

            LocalBroadcastManager.getInstance(this@GlyphPlaybackService).sendBroadcast(
                Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
                    putExtra(EXTRA_IS_PLAYING, true)
                    putExtra(EXTRA_PLAYLIST_ID, playlistId)
                }
            )

            updateAllWidgets(this@GlyphPlaybackService, isPlaying = true,
                playlistId = playlistId, playlistName = playlist.playlist.name)

            try {
                // Give GlyphManager a moment to connect during cold-starts
                delay(300)
                
                while (isActive) {
                    // ── Drift-corrected clock (matches GlyphController.playSequence) ──
                    var expectedTimeMs = android.os.SystemClock.elapsedRealtime()

                    for (step in playlist.steps) {
                        if (!isActive) break

                        glyphController.applyGlyphStateWithIntensities(
                            step.channelIntensities, step.durationMs, GlyphController.GlyphOwner.WIDGET)

                        // Fire widget update asynchronously — don't let I/O eat into step time
                        val intensities = glyphController.channels.map {
                            step.channelIntensities[it] ?: 0
                        }
                        launch { updateAllWidgets(this@GlyphPlaybackService, intensities = intensities) }

                        // Drift correction: sleep only the remaining time, not a flat delay
                        expectedTimeMs += step.durationMs.toLong()
                        val remaining = expectedTimeMs - android.os.SystemClock.elapsedRealtime()
                        if (remaining > 0) delay(remaining)
                    }
                }
            } finally {
                glyphController.turnOffGlyphs()
                updateAllWidgets(this@GlyphPlaybackService, isPlaying = false)
                LocalBroadcastManager.getInstance(this@GlyphPlaybackService).sendBroadcast(
                    Intent(ACTION_PLAYBACK_STATE_CHANGED).apply {
                        putExtra(EXTRA_IS_PLAYING, false)
                    }
                )
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun stopPlayback() {
        Log.d("GlyphPlaybackService", "stopPlayback")
        playbackJob?.cancel()
        playbackJob = null

        // Reset widget state on stop
        serviceScope.launch {
            updateAllWidgets(this@GlyphPlaybackService, isPlaying = false)
            stopSelf()
        }
    }

    private fun createNotification(name: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Playing Glyph Sequence")
            .setContentText(name)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Glyph Playback",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val CHANNEL_ID = "glyph_playback_channel"
        private const val NOTIFICATION_ID = 1003

        const val ACTION_START = "com.smaarig.glyphbarcomposer.action.START_PLAYBACK"
        const val ACTION_STOP = "com.smaarig.glyphbarcomposer.action.STOP_PLAYBACK"
        const val ACTION_PLAYBACK_STATE_CHANGED = "com.smaarig.glyphbarcomposer.PLAYBACK_STATE"
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
    }
}
