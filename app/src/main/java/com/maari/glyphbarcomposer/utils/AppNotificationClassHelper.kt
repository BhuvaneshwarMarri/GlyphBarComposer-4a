package com.maari.glyphbarcomposer.utils

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

    fun isProgressOnlyApp(packageName: String): Boolean {
        return packageName in PROGRESS_ONLY_PACKAGES
    }

    fun getChannelsForPackage(context: Context, packageName: String): List<AppNotificationChannel> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return emptyList()

        return try {
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

    fun formatChannelName(channel: AppNotificationChannel): String {
        return channel.name.ifBlank { channel.id }
    }

    fun importanceLabel(importance: Int): String = when (importance) {
        NotificationManager.IMPORTANCE_HIGH -> "Urgent"
        NotificationManager.IMPORTANCE_DEFAULT -> "Default"
        NotificationManager.IMPORTANCE_LOW -> "Low"
        NotificationManager.IMPORTANCE_MIN -> "Minimal"
        NotificationManager.IMPORTANCE_NONE -> "None"
        else -> "Unknown"
    }
}
