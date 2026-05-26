package com.smaarig.glyphbarcomposer.service

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Process
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import kotlinx.coroutines.*
import kotlin.collections.filter

class GlyphNotificationListenerService : NotificationListenerService() {
    private val TAG = "GlyphNotificationService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Track active sequence jobs per hook so they don't stack
    private val activeJobs = mutableMapOf<Long, Job>()
    private var glyphController: GlyphController? = null

    // Tracking for progress-sync restoration
    private var lastProgressSbn: StatusBarNotification? = null
    private var lastProgressPlaylistId: Long? = null

    private var mediaSessionManager: MediaSessionManager? = null
    private val activeMediaControllers = mutableMapOf<String, MediaController>()
    private var progressPollingJob: Job? = null

    companion object {
        private var instance: GlyphNotificationListenerService? = null

        fun getActiveChannels(packageName: String): List<NotificationChannel> {
            val service = instance
            if (service == null) {
                Log.w("GlyphNotificationService", "Service instance not available")
                return emptyList()
            }
            return try {
                service.getNotificationChannels(packageName, Process.myUserHandle())
            } catch (e: Exception) {
                Log.e("GlyphNotificationService", "Failed to get channels for $packageName: ${e.message}")
                emptyList()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        glyphController = GlyphController.getInstance(this)
        mediaSessionManager = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        Log.d(TAG, "Notification Listener Service Created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "Notification Listener Connected")
        setupMediaSessionListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.d(TAG, "Notification Listener Disconnected")
        stopProgressPolling()
    }

    private fun setupMediaSessionListener() {
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            activeMediaControllers.clear()
            controllers?.forEach { controller ->
                activeMediaControllers[controller.packageName] = controller
            }
            Log.d(TAG, "Media Sessions Updated: ${activeMediaControllers.keys}")
            
            // If we have a tracked progress hook, restart polling if needed
            val sbn = lastProgressSbn
            if (sbn != null && activeMediaControllers.containsKey(sbn.packageName)) {
                startProgressPolling()
            }
        }

        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(
                listener,
                android.content.ComponentName(this, GlyphNotificationListenerService::class.java)
            )
            
            // Initial load
            mediaSessionManager?.getActiveSessions(
                android.content.ComponentName(this, GlyphNotificationListenerService::class.java)
            )?.forEach { controller ->
                activeMediaControllers[controller.packageName] = controller
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to setup MediaSession listener: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val packageName = sbn.packageName
        Log.d(TAG, "Notification Posted from: $packageName")

        serviceScope.launch {
            val app = application as GlyphApplication
            val repository = app.repository

            // Only fetch ENABLED hooks for this package
            val hooks = repository.getHooksForPackage(packageName)

            if (hooks.isEmpty()) {
                Log.d(TAG, "No active hooks for: $packageName")
                return@launch
            }

            for (hookWithPlaylist in hooks) {
                val hook = hookWithPlaylist.hook
                val playlist = hookWithPlaylist.playlist ?: continue

                Log.d(TAG, "Evaluating hook ${hook.id} | type=${hook.notificationType} | channelId=${hook.notificationChannelId}")

                // --- Notification channel ID matching (most precise) ---
                if (!hook.notificationChannelId.isNullOrEmpty()) {
                    val sbnChannelId = sbn.notification.channelId
                    if (sbnChannelId != hook.notificationChannelId) {
                        Log.d(TAG, "Channel mismatch: got=$sbnChannelId expected=${hook.notificationChannelId}")
                        continue
                    }
                } else if (hook.notificationType != "ALL") {
                    // Fall back to category matching only if no channel ID is set
                    if (!matchesCategory(sbn, hook.notificationType)) {
                        Log.d(TAG, "Category mismatch for $packageName: expected ${hook.notificationType}")
                        continue
                    }
                }

                Log.d(TAG, "Hook matched! Triggering for ${hook.packageName}")

                if (hook.isProgressSync) {
                    lastProgressSbn = sbn
                    lastProgressPlaylistId = playlist.id
                    startProgressPolling()
                } else {
                    val playlistWithSteps = repository.getPlaylistWithSteps(playlist.id)
                    playlistWithSteps?.let {
                        // Cancel any existing sequence for this hook before starting a new one
                        activeJobs[hook.id]?.cancel()
                        activeJobs[hook.id] = serviceScope.launch {
                            playSequence(it)
                        }
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification Removed: ${sbn.packageName}")
        
        // If the removed notification was our tracked progress notification, turn off glyphs
        if (sbn.packageName == lastProgressSbn?.packageName && 
            sbn.id == lastProgressSbn?.id && 
            sbn.tag == lastProgressSbn?.tag) {
            
            Log.d(TAG, "Progress notification removed, turning off glyphs")
            lastProgressSbn = null
            lastProgressPlaylistId = null
            stopProgressPolling()
            glyphController?.turnOffGlyphs()
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = serviceScope.launch {
            while (isActive) {
                val sbn = lastProgressSbn ?: break
                val playlistId = lastProgressPlaylistId ?: break
                
                val controller = activeMediaControllers[sbn.packageName]
                if (controller != null) {
                    handleMediaProgress(controller, playlistId)
                } else {
                    // Fallback to legacy extras if no MediaSession
                    handleLegacyProgress(sbn, playlistId)
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    private fun handleMediaProgress(controller: MediaController, playlistId: Long) {
        val metadata = controller.metadata
        val state = controller.playbackState
        
        if (metadata == null || state == null) return
        
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        var position = state.position

        // Standard interpolation for media position
        if (state.state == PlaybackState.STATE_PLAYING && state.playbackSpeed > 0) {
            val timeDiff = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            position += (timeDiff * state.playbackSpeed).toLong()
        }
        
        if (duration <= 0) return
        
        val percentage = (position * 100 / duration).toInt().coerceIn(0, 100)
        applyProgressToGlyphs(percentage, playlistId)
    }

    private fun handleLegacyProgress(sbn: StatusBarNotification, playlistId: Long) {
        val extras = sbn.notification.extras
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)

        if (progressMax > 0 && progress >= 0) {
            val percentage = (progress * 100 / progressMax)
            applyProgressToGlyphs(percentage, playlistId)
        }
    }

    private fun applyProgressToGlyphs(percentage: Int, playlistId: Long) {
        serviceScope.launch {
            val app = application as GlyphApplication
            val playlistWithSteps = app.repository.getPlaylistWithSteps(playlistId)
            playlistWithSteps?.let {
                val steps = it.steps.sortedBy { s -> s.stepIndex }
                if (steps.isNotEmpty()) {
                    val stepIndex = (percentage * (steps.size - 1) / 100).coerceIn(0, steps.size - 1)
                    val targetStep = steps[stepIndex]
                    glyphController?.applyGlyphStateWithIntensities(
                        targetStep.channelIntensities,
                        1000 // Polling interval
                    )
                }
            }
        }
    }

    /**
     * Category-based matching as a fallback when no specific channel ID is configured.
     */
    private fun matchesCategory(sbn: StatusBarNotification, categoryId: String): Boolean {
        val notification = sbn.notification
        return when (categoryId) {
            "MESSAGES" -> notification.category == Notification.CATEGORY_MESSAGE
            "CALLS"    -> notification.category == Notification.CATEGORY_CALL || notification.fullScreenIntent != null
            "SOCIAL"   -> notification.category == Notification.CATEGORY_SOCIAL
            "ALERTS"   -> notification.category == Notification.CATEGORY_ALARM || notification.category == Notification.CATEGORY_REMINDER
            "SYSTEM"   -> notification.category == Notification.CATEGORY_SYSTEM || notification.category == Notification.CATEGORY_STATUS
            "DOWNLOADS","PROGRESS" ->
                notification.category == Notification.CATEGORY_PROGRESS ||
                        notification.extras.containsKey(Notification.EXTRA_PROGRESS)
            else -> true
        }
    }

    /**
     * Plays all steps in order, with strict duration limits:
     * - Max total duration: 1.5 seconds.
     * - If the sequence is larger than 1.5s, it's trimmed to exactly 1.0 second.
     * - Turns off glyphs (or restores progress) after playing.
     */
    private suspend fun playSequence(playlistWithSteps: PlaylistWithSteps) {
        val steps = playlistWithSteps.steps.sortedBy { it.stepIndex }
        val rawTotalDuration = steps.sumOf { it.durationMs.toLong() }
        
        // Requirement: max 1.5s, larger trimmed to 1s.
        val allowedDuration = if (rawTotalDuration > 1500) 1000L else rawTotalDuration
        
        Log.d(TAG, "Playing sequence: raw=${rawTotalDuration}ms, allowed=${allowedDuration}ms")

        var elapsed = 0L
        for (step in steps) {
            yield() // Check for cancellation
            
            val remaining = allowedDuration - elapsed
            if (remaining <= 0) break
            
            val durationToPlay = minOf(step.durationMs.toLong(), remaining)
            
            glyphController?.applyGlyphStateWithIntensities(
                step.channelIntensities,
                durationToPlay.toInt()
            )
            
            delay(durationToPlay)
            elapsed += durationToPlay
        }
        
        // After playing the sequence, turn off or restore progress
        restoreProgressOrTurnOff()
    }

    private fun restoreProgressOrTurnOff() {
        val sbn = lastProgressSbn
        val playlistId = lastProgressPlaylistId
        
        if (sbn != null && playlistId != null) {
            Log.d(TAG, "Restoring progress bar for ${sbn.packageName}")
            startProgressPolling()
        } else {
            Log.d(TAG, "No active progress, turning off glyphs")
            glyphController?.turnOffGlyphs()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeJobs.values.forEach { it.cancel() }
        stopProgressPolling()
        serviceScope.cancel()
        instance = null
    }
}
