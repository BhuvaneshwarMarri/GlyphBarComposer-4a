package com.smaarig.glyphbarcomposer.utils

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

data class AppNotificationChannel(
    val id: String,
    val name: String,
    val description: String?,
    val importance: Int
)

/**
 * Known media/progress-only packages that should be restricted to progress-sync mode only.
 * These apps use notification progress to represent playback position or download state.
 */
val PROGRESS_ONLY_PACKAGES = setOf(
    "com.spotify.music",
    "com.google.android.youtube",
    "com.google.android.apps.youtube.music",
    "com.amazon.mp3",
    "com.soundcloud.android",
    "com.pandora.android",
    "com.apple.android.music",
    "com.deezer.android",
    "com.tidal.music",
    "com.jio.media.jiomusic",
    "com.gaana",
    "in.wynk.music",
    "com.download.manager",
    "com.android.providers.downloads",
    "com.microsoft.teams"
)

object AppNotificationChannelHelper {

    private const val TAG = "AppChannelHelper"

    /**
     * Returns true if this package is a media/progress app that should only use
     * progress-sync mode. The UI should hide the "regular trigger" checkbox for these.
     */
    fun isProgressOnlyApp(packageName: String): Boolean {
        return packageName in PROGRESS_ONLY_PACKAGES
    }

    /**
     * Reads the notification channels registered by [packageName].
     * Returns an empty list on Android < O or if the app has no channels.
     *
     * Requires that the calling app holds BIND_NOTIFICATION_LISTENER_SERVICE permission
     * (already declared in your manifest), OR uses createPackageContext to read channel info
     * via NotificationManager obtained from the target package's context.
     *
     * NOTE: Android does NOT expose other apps' channels via a public API directly.
     * We use NotificationListenerService's getNotificationChannels() (API 26+).
     * The service must be active for this to work; call from the ViewModel after
     * passing the listener service reference, or use the workaround below via
     * the package's context.
     */
    fun getChannelsForPackage(context: Context, packageName: String): List<AppNotificationChannel> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        return try {
            // Create a context for the target package so we can read its NM channels
            val targetContext = context.createPackageContext(
                packageName,
                Context.CONTEXT_IGNORE_SECURITY
            )
            val nm =
                targetContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.notificationChannels?.mapNotNull { ch ->
                if (ch.id.isNullOrEmpty()) null
                else AppNotificationChannel(
                    id = ch.id,
                    name = ch.name?.toString() ?: ch.id,
                    description = ch.description,
                    importance = ch.importance
                )
            }?.sortedBy { it.name } ?: emptyList()
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found: $packageName")
            emptyList()
        } catch (e: SecurityException) {
            Log.w(TAG, "Security exception reading channels for $packageName: ${e.message}")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading channels for $packageName: ${e.message}")
            emptyList()
        }
    }

    /**
     * Returns a user-friendly display name for a notification channel.
     */
    fun formatChannelName(channel: AppNotificationChannel): String {
        return channel.name.ifBlank { channel.id }
    }

    /**
     * Returns importance label for display.
     */
    fun importanceLabel(importance: Int): String = when (importance) {
        NotificationManager.IMPORTANCE_HIGH -> "Urgent"
        NotificationManager.IMPORTANCE_DEFAULT -> "Default"
        NotificationManager.IMPORTANCE_LOW -> "Low"
        NotificationManager.IMPORTANCE_MIN -> "Minimal"
        NotificationManager.IMPORTANCE_NONE -> "None"
        else -> "Unknown"
    }
}