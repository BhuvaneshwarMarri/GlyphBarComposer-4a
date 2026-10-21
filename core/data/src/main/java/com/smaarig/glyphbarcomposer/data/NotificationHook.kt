package com.smaarig.glyphbarcomposer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a trigger: when a notification from [packageName] arrives on
 * [notificationChannelId] (or matching [notificationType] category), play [playlistId].
 *
 * [notificationChannelId] — the Android notification channel ID of the target app's channel.
 *   When set, this takes priority over [notificationType] for matching.
 *   When null/empty, the legacy category-based [notificationType] matching is used.
 *
 * [notificationType] — broad category bucket: "ALL", "MESSAGES", "CALLS", etc.
 *   Used as fallback when [notificationChannelId] is not set.
 *
 * [isProgressSync] — if true, the hook maps the notification's progress bar (0–100%)
 *   directly to a step in the playlist. Suitable for Spotify, YouTube, downloads, etc.
 *   When true, the hook UI should ONLY show progress-sync mode (no regular trigger).
 *
 * [isProgressOnly] — if true, this app only supports progress-sync mode (media/download apps).
 *   The UI should hide the "regular trigger" option.
 */
@Entity(
    tableName = "notification_hooks",
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("packageName")]
)
data class NotificationHook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val packageName: String,
    val appName: String,
    val playlistId: Long? = null,
    val presetName: String? = null,
    val isEnabled: Boolean = true,
    val isProgressSync: Boolean = false,

    /** Broad category fallback: "ALL", "MESSAGES", "CALLS", "SOCIAL", "ALERTS", "SYSTEM", "DOWNLOADS" */
    val notificationType: String = "ALL",

    /** Precise Android notification channel ID — takes priority over notificationType when set */
    val notificationChannelId: String? = null,

    /** Human-readable name of the channel (for display in UI) */
    val notificationChannelName: String? = null,

    /** Extra metadata (reserved for future use) */
    val extraData: String? = null
)