package com.smaarig.glyphbarcomposer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Process
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.R
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.NotificationHookWithPlaylist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

class GlyphNotificationListenerService : NotificationListenerService() {

    private val TAG = "GlyphNotificationService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val activeJobs = mutableMapOf<Long, Job>()
    private var glyphController: GlyphController? = null

    private var lastProgressSbn: StatusBarNotification? = null
    private var lastProgressHookId: Long? = null

    private var mediaSessionManager: MediaSessionManager? = null
    private val activeMediaControllers = mutableMapOf<String, MediaController>()
    private var progressPollingJob: Job? = null

    companion object {
        private var instance: GlyphNotificationListenerService? = null
        private const val SYNC_CHANNEL_ID = "notification_sync_channel"
        private const val SYNC_NOTIFICATION_ID = 1002

        const val ACTION_START_FOREGROUND = "com.smaarig.glyphbarcomposer.START_FOREGROUND"
        const val ACTION_STOP_FOREGROUND = "com.smaarig.glyphbarcomposer.STOP_FOREGROUND"

        fun getActiveChannels(packageName: String): List<NotificationChannel> {
            return try {
                instance?.getNotificationChannels(packageName, Process.myUserHandle())
                    ?: emptyList()
            } catch (e: Exception) {
                Log.e(
                    "GlyphNotificationService",
                    "Failed to get channels for $packageName: ${e.message}"
                )
                emptyList()
            }
        }

        /**
         * Called when a hook is deleted or toggled OFF.
         * Cancels any active glyph sequence job for that hook and turns off glyphs
         * if no progress tracking is running.
         */
        fun cancelHookPlayback(hookId: Long) {
            val svc = instance ?: return
            svc.activeJobs[hookId]?.cancel()
            svc.activeJobs.remove(hookId)

            if (svc.lastProgressHookId == hookId) {
                svc.lastProgressSbn = null
                svc.lastProgressHookId = null
                svc.stopProgressPolling()
                svc.glyphController?.turnOffGlyphs()
            } else if (svc.activeJobs.isEmpty() && svc.lastProgressSbn == null) {
                svc.glyphController?.turnOffGlyphs()
            }
            Log.d("GlyphNotificationService", "Cancelled playback for hook $hookId")
        }

        /**
         * Directly triggers the glyph sequence for a hook — used by the "Test" button.
         * Returns true if the service was available and the sequence was dispatched.
         */
        fun triggerTestForHook(hookWithPlaylist: NotificationHookWithPlaylist): Boolean {
            val svc = instance ?: return false
            val hook = hookWithPlaylist.hook

            svc.activeJobs[hook.id]?.cancel()
            svc.activeJobs[hook.id] = svc.serviceScope.launch {
                if (hook.isProgressSync) {
                    // Simulate 50% progress for testing
                    svc.applyProgressToGlyphs(50, hook)
                    delay(5000) // Auto-off after 5 seconds
                    svc.glyphController?.turnOffGlyphs()
                } else {
                    val playlist = hookWithPlaylist.playlist ?: return@launch
                    val app = svc.application as GlyphApplication
                    val playlistWithSteps =
                        app.repository.getPlaylistWithSteps(playlist.id) ?: return@launch
                    svc.playSequence(playlistWithSteps)
                }
            }
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        glyphController = GlyphController.getInstance(this)
        mediaSessionManager =
            getSystemService(MEDIA_SESSION_SERVICE) as? MediaSessionManager
        Log.d(TAG, "Service Created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> startForegroundSync()
            ACTION_STOP_FOREGROUND -> stopForegroundSync()
        }
        return START_STICKY
    }

    private fun startForegroundSync() {
        createNotificationChannel()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                SYNC_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(SYNC_NOTIFICATION_ID, notification)
        }
        Log.d(TAG, "Started Foreground Sync")
    }

    private fun stopForegroundSync() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        Log.d(TAG, "Stopped Foreground Sync")
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, SYNC_CHANNEL_ID)
            .setContentTitle("Glyph Sync Active")
            .setContentText("Listening for notification hooks in background")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Using app icon
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                SYNC_CHANNEL_ID, "Glyph Notification Sync Channel",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        Log.d(TAG, "Listener Connected")

        // Check if we should be in foreground
        val prefs = getSharedPreferences("glyph_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("bg_sync_enabled", false)) {
            startForegroundSync()
        }

        setupMediaSessionListener()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        Log.d(TAG, "Listener Disconnected")
        stopProgressPolling()
    }

    private fun setupMediaSessionListener() {
        val componentName =
            android.content.ComponentName(this, GlyphNotificationListenerService::class.java)

        val listener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            activeMediaControllers.clear()
            controllers?.forEach { activeMediaControllers[it.packageName] = it }
            val sbn = lastProgressSbn
            if (sbn != null && activeMediaControllers.containsKey(sbn.packageName)) {
                startProgressPolling()
            }
        }

        try {
            mediaSessionManager?.addOnActiveSessionsChangedListener(listener, componentName)
            mediaSessionManager?.getActiveSessions(componentName)
                ?.forEach { activeMediaControllers[it.packageName] = it }
        } catch (e: Exception) {
            Log.e(TAG, "MediaSession setup failed: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        val packageName = sbn.packageName
        Log.d(TAG, "Notification from: $packageName")

        serviceScope.launch {
            val app = application as GlyphApplication
            val hooks = try {
                app.repository.getHooksForPackage(packageName)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching hooks for $packageName: ${e.message}")
                return@launch
            }

            if (hooks.isEmpty()) return@launch

            for (hookWithPlaylist in hooks) {
                val hook = hookWithPlaylist.hook

                // Channel ID matching (highest priority)
                if (!hook.notificationChannelId.isNullOrEmpty()) {
                    val sbnChannelId = sbn.notification.channelId
                    if (sbnChannelId != hook.notificationChannelId) continue
                } else if (hook.notificationType != "ALL") {
                    if (!matchesCategory(sbn, hook.notificationType)) continue
                }

                Log.d(TAG, "Hook ${hook.id} matched for $packageName")

                if (hook.isProgressSync) {
                    lastProgressSbn = sbn
                    lastProgressHookId = hook.id
                    startProgressPolling()
                } else {
                    val playlistId = hook.playlistId ?: continue
                    val playlistWithSteps = try {
                        app.repository.getPlaylistWithSteps(playlistId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching playlist $playlistId: ${e.message}")
                        null
                    } ?: continue

                    activeJobs[hook.id]?.cancel()
                    activeJobs[hook.id] = serviceScope.launch {
                        playSequence(playlistWithSteps)
                    }
                }
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
        Log.d(TAG, "Notification removed: ${sbn.packageName}")

        if (sbn.packageName == lastProgressSbn?.packageName &&
            sbn.id == lastProgressSbn?.id &&
            sbn.tag == lastProgressSbn?.tag
        ) {
            Log.d(TAG, "Progress notification removed — turning off glyphs")
            lastProgressSbn = null
            lastProgressHookId = null
            stopProgressPolling()
            glyphController?.turnOffGlyphs()
        }
    }

    private fun startProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = serviceScope.launch {
            while (isActive) {
                val sbn = lastProgressSbn ?: break
                val hookId = lastProgressHookId ?: break

                val app = application as GlyphApplication
                val hook = app.repository.getNotificationHookSync(hookId) ?: break

                val controller = activeMediaControllers[sbn.packageName]

                if (controller != null) {
                    handleMediaProgress(controller, hook)
                } else {
                    handleLegacyProgress(sbn, hook)
                }
                delay(1000)
            }
        }
    }

    private fun stopProgressPolling() {
        progressPollingJob?.cancel()
        progressPollingJob = null
    }

    private fun handleMediaProgress(
        controller: MediaController,
        hook: com.smaarig.glyphbarcomposer.data.NotificationHook
    ) {
        val metadata = controller.metadata ?: return
        val state = controller.playbackState ?: return

        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)
        var position = state.position

        if (state.state == PlaybackState.STATE_PLAYING && state.playbackSpeed > 0) {
            val timeDiff = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
            position += (timeDiff * state.playbackSpeed).toLong()
        }

        if (duration <= 0) return
        val percentage = (position * 100 / duration).toInt().coerceIn(0, 100)
        applyProgressToGlyphs(percentage, hook)
    }

    private fun handleLegacyProgress(
        sbn: StatusBarNotification,
        hook: com.smaarig.glyphbarcomposer.data.NotificationHook
    ) {
        val extras = sbn.notification.extras
        val progress = extras.getInt(Notification.EXTRA_PROGRESS, -1)
        val progressMax = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        if (progressMax > 0 && progress >= 0) {
            applyProgressToGlyphs(progress * 100 / progressMax, hook)
        }
    }

    internal fun applyProgressToGlyphs(
        percentage: Int,
        hook: com.smaarig.glyphbarcomposer.data.NotificationHook
    ) {
        if (hook.isProgressSync) {
            glyphController?.applySmoothProgress(percentage)
        } else {
            val playlistId = hook.playlistId ?: return
            serviceScope.launch {
                val app = application as GlyphApplication
                val playlistWithSteps = try {
                    app.repository.getPlaylistWithSteps(playlistId)
                } catch (e: Exception) {
                    Log.e(TAG, "Error applying progress: ${e.message}")
                    null
                } ?: return@launch

                val steps = playlistWithSteps.steps.sortedBy { it.stepIndex }
                if (steps.isEmpty()) return@launch

                val stepIndex = (percentage * (steps.size - 1) / 100).coerceIn(0, steps.size - 1)
                glyphController?.applyGlyphStateWithIntensities(
                    steps[stepIndex].channelIntensities,
                    1000
                )
            }
        }
    }

    private fun matchesCategory(sbn: StatusBarNotification, categoryId: String): Boolean {
        val notification = sbn.notification
        return when (categoryId) {
            "MESSAGES" -> notification.category == Notification.CATEGORY_MESSAGE
            "CALLS" -> notification.category == Notification.CATEGORY_CALL || notification.fullScreenIntent != null
            "SOCIAL" -> notification.category == Notification.CATEGORY_SOCIAL
            "ALERTS" -> notification.category == Notification.CATEGORY_ALARM || notification.category == Notification.CATEGORY_REMINDER
            "SYSTEM" -> notification.category == Notification.CATEGORY_SYSTEM || notification.category == Notification.CATEGORY_STATUS
            "DOWNLOADS", "PROGRESS" ->
                notification.category == Notification.CATEGORY_PROGRESS ||
                        notification.extras.containsKey(Notification.EXTRA_PROGRESS)

            else -> true
        }
    }

    internal suspend fun playSequence(playlistWithSteps: PlaylistWithSteps) {
        val steps = playlistWithSteps.steps.sortedBy { it.stepIndex }
        val rawTotalDuration = steps.sumOf { it.durationMs.toLong() }
        val allowedDuration = if (rawTotalDuration > 1500) 1000L else rawTotalDuration

        Log.d(TAG, "Playing sequence: raw=${rawTotalDuration}ms, allowed=${allowedDuration}ms")

        var elapsed = 0L
        for (step in steps) {
            yield()
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

        restoreProgressOrTurnOff()
    }

    private fun restoreProgressOrTurnOff() {
        val sbn = lastProgressSbn
        val hookId = lastProgressHookId
        if (sbn != null && hookId != null) {
            startProgressPolling()
        } else {
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