package com.smaarig.glyphbarcomposer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.R
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.ui.widget.DEFAULT_INTENSITIES
import com.smaarig.glyphbarcomposer.ui.widget.GlyphSequencePlayerWidget
import com.smaarig.glyphbarcomposer.ui.widget.INTENSITIES_KEY
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

            // Show foreground notification
            startForeground(NOTIFICATION_ID, createNotification(playlist.playlist.name))

            // Update widget state to PLAYING
            updateAllWidgets(this@GlyphPlaybackService, isPlaying = true, playlistId = playlistId, playlistName = playlist.playlist.name)

            // BUG-3 FIX: Sort steps by stepIndex. Room @Relation does not guarantee ordering,
            // so without sorting the sequence can play in a scrambled order.
            val sortedSteps = playlist.steps.sortedBy { it.stepIndex }

            try {
                // BUG-2 FIX: Use a drift-correcting timing loop (same approach as
                // GlyphController.playSequence). The old `delay(durationMs + 50)` added
                // 50 ms of extra delay per step — a 100-step sequence drifted by 5 seconds.
                var expectedTimeMs = android.os.SystemClock.elapsedRealtime()

                while (isActive) {
                    for (step in sortedSteps) {
                        if (!isActive) break

                        // BUG-4 FIX: Pass GlyphOwner.STUDIO so this call is never rejected
                        // by the ownership guard when a screen holds transient control.
                        glyphController.applyGlyphStateWithIntensities(
                            step.channelIntensities,
                            step.durationMs,
                            GlyphController.GlyphOwner.STUDIO
                        )

                        // Update widget visualization (mini glyph bar).
                        // applyGlyphStateWithIntensities already calls maybeUpdateWidgets
                        // internally (now throttled to 250ms).
                        expectedTimeMs += step.durationMs.toLong()
                        val remaining = expectedTimeMs - android.os.SystemClock.elapsedRealtime()
                        if (remaining > 0) delay(remaining)
                    }
                }
            } finally {
                glyphController.turnOffGlyphs()
                updateAllWidgets(this@GlyphPlaybackService, isPlaying = false)
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
        private const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.smaarig.glyphbarcomposer.action.START_PLAYBACK"
        const val ACTION_STOP = "com.smaarig.glyphbarcomposer.action.STOP_PLAYBACK"
        const val EXTRA_PLAYLIST_ID = "extra_playlist_id"
    }
}