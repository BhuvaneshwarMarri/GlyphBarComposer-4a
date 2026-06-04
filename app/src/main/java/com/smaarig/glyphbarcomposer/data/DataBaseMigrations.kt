package com.smaarig.glyphbarcomposer.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration from DB version 2 → 3.
 *
 * Adds [notificationChannelId] and [notificationChannelName] to notification_hooks.
 * Both are nullable so existing rows are unaffected.
 *
 * Apply in your RoomDatabase builder:
 *
 *   Room.databaseBuilder(...)
 *       .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
 *       .build()
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE notification_hooks ADD COLUMN notificationChannelId TEXT DEFAULT NULL"
        )
        database.execSQL(
            "ALTER TABLE notification_hooks ADD COLUMN notificationChannelName TEXT DEFAULT NULL"
        )
    }
}

/**
 * Migration from 3 → 13.
 * Covers all features added during development: Music Studio, Progress Sync, etc.
 */
val MIGRATION_3_13 = object : Migration(3, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 1. Playlists changes
        database.execSQL("ALTER TABLE playlists ADD COLUMN isProgressSequence INTEGER NOT NULL DEFAULT 0")

        // 2. Notification Hooks changes
        database.execSQL("ALTER TABLE notification_hooks ADD COLUMN appName TEXT NOT NULL DEFAULT ''")
        database.execSQL("ALTER TABLE notification_hooks ADD COLUMN isProgressSync INTEGER NOT NULL DEFAULT 0")

        // 3. Music Studio tables
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS music_studio_projects (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                localAudioPath TEXT NOT NULL,
                localGlyphPath TEXT
            )
        """)
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS music_studio_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                projectId INTEGER NOT NULL,
                timestampMs INTEGER NOT NULL,
                channelIntensities TEXT NOT NULL,
                durationMs INTEGER NOT NULL DEFAULT 100,
                FOREIGN KEY(projectId) REFERENCES music_studio_projects(id) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """)
        database.execSQL("CREATE INDEX IF NOT EXISTS index_music_studio_events_projectId ON music_studio_events (projectId)")

        // 4. Contact Bindings table
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS contact_bindings (
                contactId TEXT PRIMARY KEY NOT NULL,
                contactName TEXT NOT NULL,
                playlistId INTEGER NOT NULL
            )
        """)
    }
}

/**
 * If you are starting from version 1 → 3 (skipping 2), use this combined migration instead.
 */
val MIGRATION_1_3 = object : Migration(1, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Add all columns added in v2 + v3 here
        // (adjust this list to match your actual v2 migration)
        database.execSQL(
            "ALTER TABLE notification_hooks ADD COLUMN notificationType TEXT NOT NULL DEFAULT 'ALL'"
        )
        database.execSQL(
            "ALTER TABLE notification_hooks ADD COLUMN extraData TEXT DEFAULT NULL"
        )
        database.execSQL(
            "ALTER TABLE notification_hooks ADD COLUMN notificationChannelId TEXT DEFAULT NULL"
        )
        database.execSQL(
            "ALTER TABLE notification_hooks ADD COLUMN notificationChannelName TEXT DEFAULT NULL"
        )
    }
}