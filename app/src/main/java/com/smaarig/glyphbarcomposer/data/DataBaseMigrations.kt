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