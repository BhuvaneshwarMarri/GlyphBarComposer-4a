package com.smaarig.glyphbarcomposer.service

import android.app.Notification
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

    override fun onCreate() {
        super.onCreate()
        glyphController = GlyphController.getInstance(this)
        Log.d(TAG, "Notification Listener Service Created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val packageName = sbn.packageName
        Log.d(TAG, "Notification Posted from: $packageName")

        serviceScope.launch {
            val app = application as GlyphApplication
            val repository = app.repository

            // Only fetch ENABLED hooks for this package
            val hooks = repository.getHooksForPackage(packageName).filter { it.hook.isEnabled }

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
                    handleProgressNotification(sbn, playlist.id)
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
            glyphController?.turnOffGlyphs()
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
     * Maps the notification's progress value (0–100) to a step in the playlist
     * and applies that glyph state. Only fires when progressMax > 0.
     */
    private fun handleProgressNotification(sbn: StatusBarNotification, playlistId: Long) {
        val extras = sbn.notification.extras
        val progress    = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)

        if (progressMax <= 0 || progress < 0) {
            Log.d(TAG, "Progress data unavailable for ${sbn.packageName}")
            return
        }

        val percentage = (progress * 100) / progressMax
        Log.d(TAG, "Progress Update: $percentage% for ${sbn.packageName}")

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
                        targetStep.durationMs
                    )
                }
            }
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
            handleProgressNotification(sbn, playlistId)
        } else {
            Log.d(TAG, "No active progress, turning off glyphs")
            glyphController?.turnOffGlyphs()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeJobs.values.forEach { it.cancel() }
        serviceScope.cancel()
    }
}