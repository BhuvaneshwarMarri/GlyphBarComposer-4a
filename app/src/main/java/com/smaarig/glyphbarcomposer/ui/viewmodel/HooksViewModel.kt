package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smaarig.glyphbarcomposer.data.NotificationHook
import com.smaarig.glyphbarcomposer.data.NotificationHookWithPlaylist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.repository.GlyphRepository
import com.smaarig.glyphbarcomposer.service.GlyphNotificationListenerService
import com.smaarig.glyphbarcomposer.utils.AppNotificationChannel
import com.smaarig.glyphbarcomposer.utils.AppNotificationChannelHelper
import com.smaarig.glyphbarcomposer.utils.PROGRESS_ONLY_PACKAGES
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: android.graphics.drawable.Drawable?,
    // Pre-decoded bitmap — converted on the IO thread during app list load
    // so the UI thread never has to call toBitmap() during a scroll frame.
    val iconBitmap: Bitmap? = null,
    val isProgressOnly: Boolean = false
)

class HooksViewModel(
    application: Application,
    private val repository: GlyphRepository
) : AndroidViewModel(application) {

    val allHooks: StateFlow<List<NotificationHookWithPlaylist>> = repository.allNotificationHooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPlaylists: StateFlow<List<PlaylistWithSteps>> = repository.allPlaylists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    private val _isBackgroundServiceEnabled = MutableStateFlow(false)
    val isBackgroundServiceEnabled: StateFlow<Boolean> = _isBackgroundServiceEnabled.asStateFlow()

    private val _selectedAppChannels = MutableStateFlow<List<AppNotificationChannel>>(emptyList())
    val selectedAppChannels: StateFlow<List<AppNotificationChannel>> =
        _selectedAppChannels.asStateFlow()

    private val _isLoadingChannels = MutableStateFlow(false)
    val isLoadingChannels: StateFlow<Boolean> = _isLoadingChannels.asStateFlow()

    /** One-shot message for test result snackbar */
    private val _testHookResult = MutableStateFlow<String?>(null)
    val testHookResult: StateFlow<String?> = _testHookResult.asStateFlow()

    init {
        loadInstalledApps()
        checkPermission()
        checkBackgroundServiceStatus()
    }

    private fun checkBackgroundServiceStatus() {
        val prefs =
            getApplication<Application>().getSharedPreferences("glyph_prefs", Context.MODE_PRIVATE)
        _isBackgroundServiceEnabled.value = prefs.getBoolean("bg_sync_enabled", false)
    }

    fun toggleBackgroundService(enabled: Boolean) {
        val context = getApplication<Application>()
        val prefs = context.getSharedPreferences("glyph_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("bg_sync_enabled", enabled).apply()
        _isBackgroundServiceEnabled.value = enabled

        val intent = Intent(context, GlyphNotificationListenerService::class.java).apply {
            action = if (enabled) {
                GlyphNotificationListenerService.ACTION_START_FOREGROUND
            } else {
                GlyphNotificationListenerService.ACTION_STOP_FOREGROUND
            }
        }

        try {
            if (enabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            _testHookResult.value = "Failed to start service: ${e.message}"
        }
    }

    fun checkPermission() {
        val context = getApplication<Application>().applicationContext
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver, "enabled_notification_listeners"
        )
        _isPermissionGranted.value = enabledListeners?.contains(context.packageName) == true
    }

    fun openPermissionSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                packages
                    .filter { info ->
                        val isSystemCore = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val hasLauncher = pm.getLaunchIntentForPackage(info.packageName) != null
                        val isKnownMedia = info.packageName in PROGRESS_ONLY_PACKAGES
                        !isSystemCore || hasLauncher || isKnownMedia
                    }
                    .mapNotNull { info ->
                        try {
                            val icon = pm.getApplicationIcon(info.packageName)
                            AppInfo(
                                packageName = info.packageName,
                                appName = pm.getApplicationLabel(info).toString(),
                                icon = icon,
                                // Decode bitmap here on the IO thread so the UI
                                // thread never calls toBitmap() during a scroll frame.
                                // asImageBitmap() in the composable is then instant.
                                iconBitmap = runCatching { icon.toBitmap() }.getOrNull(),
                                isProgressOnly = info.packageName in PROGRESS_ONLY_PACKAGES
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            null
                        }
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    fun loadChannelsForApp(packageName: String) {
        _selectedAppChannels.value = emptyList()
        _isLoadingChannels.value = true
        viewModelScope.launch {
            val channels = withContext(Dispatchers.IO) {
                val platformChannels =
                    GlyphNotificationListenerService.getActiveChannels(packageName)
                if (platformChannels.isNotEmpty()) {
                    platformChannels.map { ch ->
                        AppNotificationChannel(
                            id = ch.id,
                            name = ch.name?.toString() ?: ch.id,
                            description = ch.description,
                            importance = ch.importance
                        )
                    }.sortedBy { it.name }
                } else {
                    AppNotificationChannelHelper.getChannelsForPackage(
                        getApplication<Application>().applicationContext,
                        packageName
                    )
                }
            }
            _selectedAppChannels.value = channels
            _isLoadingChannels.value = false
        }
    }

    fun clearSelectedChannels() {
        _selectedAppChannels.value = emptyList()
    }

    fun clearTestResult() {
        _testHookResult.value = null
    }

    fun isProgressOnlyApp(packageName: String): Boolean =
        AppNotificationChannelHelper.isProgressOnlyApp(packageName)

    // ─── Hook CRUD ────────────────────────────────────────────────────────────

    fun addHook(
        packageName: String,
        appName: String,
        playlistId: Long?,
        presetName: String?,
        isProgressSync: Boolean,
        notificationType: String = "ALL",
        notificationChannelId: String? = null,
        notificationChannelName: String? = null,
        extraData: String? = null
    ) {
        viewModelScope.launch {
            val hook = NotificationHook(
                packageName = packageName,
                appName = appName,
                playlistId = playlistId,
                presetName = presetName,
                isProgressSync = isProgressSync,
                notificationType = notificationType,
                notificationChannelId = notificationChannelId,
                notificationChannelName = notificationChannelName,
                extraData = extraData
            )
            repository.saveNotificationHook(hook)
        }
    }

    fun deleteHook(hook: NotificationHook) {
        GlyphNotificationListenerService.cancelHookPlayback(hook.id)
        viewModelScope.launch { repository.deleteNotificationHook(hook) }
    }

    /**
     * Toggle a hook on/off.
     * If toggling OFF, immediately cancel any active playback for this hook
     * so glyphs don't keep running after the user disables it.
     */
    fun toggleHook(hook: NotificationHook, enabled: Boolean) {
        if (!enabled) {
            GlyphNotificationListenerService.cancelHookPlayback(hook.id)
        }
        viewModelScope.launch {
            repository.saveNotificationHook(hook.copy(isEnabled = enabled))
        }
    }

    fun updateHook(hook: NotificationHook) {
        viewModelScope.launch { repository.saveNotificationHook(hook) }
    }

    /**
     * Fires a test notification from the target app's package identity so that
     * the GlyphNotificationListenerService can match it to the hook and trigger
     * the glyph sequence. Since we can't impersonate another app, we fire from
     * our own package but simulate the structure the service expects.
     *
     * Result is reported via [testHookResult] as a user-facing string.
     */
    fun testHook(hookWithPlaylist: NotificationHookWithPlaylist, context: Context) {
        val hook = hookWithPlaylist.hook
        val playlist = hookWithPlaylist.playlist
        val presetName = hook.presetName

        if (!hook.isEnabled) {
            _testHookResult.value = "Hook is disabled — enable it first"
            return
        }
        if (playlist == null && presetName == null && !hook.isProgressSync) {
            _testHookResult.value = "No preset or sequence assigned to this hook"
            return
        }

        viewModelScope.launch {
            try {
                val testChannelId = "hook_test_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val nm =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (nm.getNotificationChannel(testChannelId) == null) {
                        val ch = NotificationChannel(
                            testChannelId,
                            "Hook Test",
                            NotificationManager.IMPORTANCE_DEFAULT
                        ).apply { description = "Used to test glyph hooks" }
                        nm.createNotificationChannel(ch)
                    }
                }

                val targetName = presetName ?: playlist?.name ?: "Unknown"
                val content =
                    if (hook.isProgressSync) "Progress Sync Test" else "Trigger Test — $targetName"

                val notification = NotificationCompat.Builder(context, testChannelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Test: ${hook.appName}")
                    .setContentText(content)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()

                val notifId = (hook.id % Int.MAX_VALUE).toInt() + 10_000
                NotificationManagerCompat.from(context).notify(notifId, notification)

                val triggered = GlyphNotificationListenerService.triggerTestForHook(
                    hookWithPlaylist
                )

                _testHookResult.value = if (triggered) {
                    val label = if (hook.isProgressSync) "Progress" else "\"$targetName\""
                    "✓ Triggered $label for ${hook.appName}"
                } else {
                    "Notification posted — make sure Glyph permission is granted"
                }
            } catch (e: SecurityException) {
                _testHookResult.value = "Permission denied — check notification permission"
            } catch (e: Exception) {
                _testHookResult.value = "Test failed: ${e.message?.take(60)}"
            }
        }
    }
}