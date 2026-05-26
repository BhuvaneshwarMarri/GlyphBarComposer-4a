package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.provider.Settings
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
    /** True if this app should only use progress-sync mode (Spotify, YouTube, etc.) */
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

    /** Holds the notification channels of the currently selected app for the channel picker sheet */
    private val _selectedAppChannels = MutableStateFlow<List<AppNotificationChannel>>(emptyList())
    val selectedAppChannels: StateFlow<List<AppNotificationChannel>> = _selectedAppChannels.asStateFlow()

    private val _isLoadingChannels = MutableStateFlow(false)
    val isLoadingChannels: StateFlow<Boolean> = _isLoadingChannels.asStateFlow()

    init {
        loadInstalledApps()
        checkPermission()
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

    /**
     * Loads all user-installed apps (including pre-loaded apps like YouTube).
     *
     * FIX: Previously used FLAG_SYSTEM which incorrectly excluded pre-installed
     * non-system apps (YouTube, Gmail, Maps). Now we keep any app that has a
     * launcher intent OR is explicitly in our known media list.
     */
    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = getApplication<Application>().packageManager

                // Use getInstalledPackages for broader coverage including stub/pre-installed apps
                val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

                packages
                    .filter { info ->
                        val isSystemCore = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val hasLauncher = pm.getLaunchIntentForPackage(info.packageName) != null
                        val isKnownMedia = info.packageName in PROGRESS_ONLY_PACKAGES

                        // Include: non-system apps, OR system apps that have a launcher,
                        // OR known media apps (to catch YouTube/Google Music if pre-installed)
                        !isSystemCore || hasLauncher || isKnownMedia
                    }
                    .mapNotNull { info ->
                        try {
                            AppInfo(
                                packageName    = info.packageName,
                                appName        = pm.getApplicationLabel(info).toString(),
                                icon           = pm.getApplicationIcon(info.packageName),
                                isProgressOnly = info.packageName in PROGRESS_ONLY_PACKAGES
                            )
                        } catch (e: PackageManager.NameNotFoundException) {
                            null // skip if icon load fails
                        }
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
        }
    }

    /**
     * Loads the real notification channels for a given app package.
     * Call this when the user taps the info/settings icon on an app card.
     */
    fun loadChannelsForApp(packageName: String) {
        _selectedAppChannels.value = emptyList()
        _isLoadingChannels.value = true
        viewModelScope.launch {
            val channels = withContext(Dispatchers.IO) {
                // Try the high-privilege Listener Service first
                val platformChannels = GlyphNotificationListenerService.getActiveChannels(packageName)
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
                    // Fall back to manual context-based lookup if service isn't connected or fails
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

    /**
     * Returns true if this package should only allow progress-sync hooks.
     */
    fun isProgressOnlyApp(packageName: String): Boolean =
        AppNotificationChannelHelper.isProgressOnlyApp(packageName)

    // ─── Hook CRUD ────────────────────────────────────────────────────────────

    /**
     * Adds a hook with full channel-level precision.
     *
     * @param notificationChannelId  The Android channel ID from [loadChannelsForApp].
     *                               Pass null to use category-level [notificationType].
     * @param notificationChannelName Human-readable channel name (for display).
     * @param notificationType        Broad category ("ALL", "MESSAGES", etc.) used when
     *                               channel ID is not available.
     * @param isProgressSync          True for Spotify/YouTube-style progress tracking.
     */
    fun addHook(
        packageName: String,
        appName: String,
        playlistId: Long,
        isProgressSync: Boolean,
        notificationType: String = "ALL",
        notificationChannelId: String? = null,
        notificationChannelName: String? = null,
        extraData: String? = null
    ) {
        viewModelScope.launch {
            val hook = NotificationHook(
                packageName           = packageName,
                appName               = appName,
                playlistId            = playlistId,
                isProgressSync        = isProgressSync,
                notificationType      = notificationType,
                notificationChannelId = notificationChannelId,
                notificationChannelName = notificationChannelName,
                extraData             = extraData
            )
            repository.saveNotificationHook(hook)
        }
    }

    fun deleteHook(hook: NotificationHook) {
        viewModelScope.launch { repository.deleteNotificationHook(hook) }
    }

    fun toggleHook(hook: NotificationHook, enabled: Boolean) {
        viewModelScope.launch { repository.saveNotificationHook(hook.copy(isEnabled = enabled)) }
    }

    fun updateHook(hook: NotificationHook) {
        viewModelScope.launch { repository.saveNotificationHook(hook) }
    }
}