package com.smaarig.glyphbarcomposer.`data`

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _playlistDao: Lazy<PlaylistDao> = lazy {
    PlaylistDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(13, "0b154592fb36f085a6a1c0d8b03a9c2c", "8fe5f5bc86854b2eb9d4aebc52fd556f") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `isProgressSequence` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `sequence_steps` (`stepId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `playlistId` INTEGER NOT NULL, `stepIndex` INTEGER NOT NULL, `channelIntensities` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_sequence_steps_playlistId` ON `sequence_steps` (`playlistId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `event_bindings` (`eventId` TEXT NOT NULL, `playlistId` INTEGER NOT NULL, `isEnabled` INTEGER NOT NULL, PRIMARY KEY(`eventId`), FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_event_bindings_playlistId` ON `event_bindings` (`playlistId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `music_studio_projects` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `localAudioPath` TEXT NOT NULL, `localGlyphPath` TEXT)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `music_studio_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `projectId` INTEGER NOT NULL, `timestampMs` INTEGER NOT NULL, `channelIntensities` TEXT NOT NULL, `durationMs` INTEGER NOT NULL, FOREIGN KEY(`projectId`) REFERENCES `music_studio_projects`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_music_studio_events_projectId` ON `music_studio_events` (`projectId`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `contact_bindings` (`contactId` TEXT NOT NULL, `contactName` TEXT NOT NULL, `playlistId` INTEGER NOT NULL, PRIMARY KEY(`contactId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `notification_hooks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `packageName` TEXT NOT NULL, `appName` TEXT NOT NULL, `playlistId` INTEGER, `presetName` TEXT, `isEnabled` INTEGER NOT NULL, `isProgressSync` INTEGER NOT NULL, `notificationType` TEXT NOT NULL, `notificationChannelId` TEXT, `notificationChannelName` TEXT, `extraData` TEXT, FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_hooks_playlistId` ON `notification_hooks` (`playlistId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_notification_hooks_packageName` ON `notification_hooks` (`packageName`)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '0b154592fb36f085a6a1c0d8b03a9c2c')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `playlists`")
        connection.execSQL("DROP TABLE IF EXISTS `sequence_steps`")
        connection.execSQL("DROP TABLE IF EXISTS `event_bindings`")
        connection.execSQL("DROP TABLE IF EXISTS `music_studio_projects`")
        connection.execSQL("DROP TABLE IF EXISTS `music_studio_events`")
        connection.execSQL("DROP TABLE IF EXISTS `contact_bindings`")
        connection.execSQL("DROP TABLE IF EXISTS `notification_hooks`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsPlaylists: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPlaylists.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaylists.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsPlaylists.put("isProgressSequence", TableInfo.Column("isProgressSequence", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPlaylists: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPlaylists: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPlaylists: TableInfo = TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists)
        val _existingPlaylists: TableInfo = read(connection, "playlists")
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |playlists(com.smaarig.glyphbarcomposer.data.Playlist).
              | Expected:
              |""".trimMargin() + _infoPlaylists + """
              |
              | Found:
              |""".trimMargin() + _existingPlaylists)
        }
        val _columnsSequenceSteps: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsSequenceSteps.put("stepId", TableInfo.Column("stepId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSequenceSteps.put("playlistId", TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSequenceSteps.put("stepIndex", TableInfo.Column("stepIndex", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSequenceSteps.put("channelIntensities", TableInfo.Column("channelIntensities", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsSequenceSteps.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysSequenceSteps: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysSequenceSteps.add(TableInfo.ForeignKey("playlists", "CASCADE", "NO ACTION", listOf("playlistId"), listOf("id")))
        val _indicesSequenceSteps: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesSequenceSteps.add(TableInfo.Index("index_sequence_steps_playlistId", false, listOf("playlistId"), listOf("ASC")))
        val _infoSequenceSteps: TableInfo = TableInfo("sequence_steps", _columnsSequenceSteps, _foreignKeysSequenceSteps, _indicesSequenceSteps)
        val _existingSequenceSteps: TableInfo = read(connection, "sequence_steps")
        if (!_infoSequenceSteps.equals(_existingSequenceSteps)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |sequence_steps(com.smaarig.glyphbarcomposer.data.SequenceStep).
              | Expected:
              |""".trimMargin() + _infoSequenceSteps + """
              |
              | Found:
              |""".trimMargin() + _existingSequenceSteps)
        }
        val _columnsEventBindings: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsEventBindings.put("eventId", TableInfo.Column("eventId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEventBindings.put("playlistId", TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsEventBindings.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysEventBindings: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysEventBindings.add(TableInfo.ForeignKey("playlists", "CASCADE", "NO ACTION", listOf("playlistId"), listOf("id")))
        val _indicesEventBindings: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesEventBindings.add(TableInfo.Index("index_event_bindings_playlistId", false, listOf("playlistId"), listOf("ASC")))
        val _infoEventBindings: TableInfo = TableInfo("event_bindings", _columnsEventBindings, _foreignKeysEventBindings, _indicesEventBindings)
        val _existingEventBindings: TableInfo = read(connection, "event_bindings")
        if (!_infoEventBindings.equals(_existingEventBindings)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |event_bindings(com.smaarig.glyphbarcomposer.data.EventBinding).
              | Expected:
              |""".trimMargin() + _infoEventBindings + """
              |
              | Found:
              |""".trimMargin() + _existingEventBindings)
        }
        val _columnsMusicStudioProjects: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMusicStudioProjects.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioProjects.put("name", TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioProjects.put("localAudioPath", TableInfo.Column("localAudioPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioProjects.put("localGlyphPath", TableInfo.Column("localGlyphPath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMusicStudioProjects: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMusicStudioProjects: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMusicStudioProjects: TableInfo = TableInfo("music_studio_projects", _columnsMusicStudioProjects, _foreignKeysMusicStudioProjects, _indicesMusicStudioProjects)
        val _existingMusicStudioProjects: TableInfo = read(connection, "music_studio_projects")
        if (!_infoMusicStudioProjects.equals(_existingMusicStudioProjects)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |music_studio_projects(com.smaarig.glyphbarcomposer.data.MusicStudioProject).
              | Expected:
              |""".trimMargin() + _infoMusicStudioProjects + """
              |
              | Found:
              |""".trimMargin() + _existingMusicStudioProjects)
        }
        val _columnsMusicStudioEvents: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMusicStudioEvents.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioEvents.put("projectId", TableInfo.Column("projectId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioEvents.put("timestampMs", TableInfo.Column("timestampMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioEvents.put("channelIntensities", TableInfo.Column("channelIntensities", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMusicStudioEvents.put("durationMs", TableInfo.Column("durationMs", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMusicStudioEvents: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysMusicStudioEvents.add(TableInfo.ForeignKey("music_studio_projects", "CASCADE", "NO ACTION", listOf("projectId"), listOf("id")))
        val _indicesMusicStudioEvents: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesMusicStudioEvents.add(TableInfo.Index("index_music_studio_events_projectId", false, listOf("projectId"), listOf("ASC")))
        val _infoMusicStudioEvents: TableInfo = TableInfo("music_studio_events", _columnsMusicStudioEvents, _foreignKeysMusicStudioEvents, _indicesMusicStudioEvents)
        val _existingMusicStudioEvents: TableInfo = read(connection, "music_studio_events")
        if (!_infoMusicStudioEvents.equals(_existingMusicStudioEvents)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |music_studio_events(com.smaarig.glyphbarcomposer.data.MusicStudioEvent).
              | Expected:
              |""".trimMargin() + _infoMusicStudioEvents + """
              |
              | Found:
              |""".trimMargin() + _existingMusicStudioEvents)
        }
        val _columnsContactBindings: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsContactBindings.put("contactId", TableInfo.Column("contactId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContactBindings.put("contactName", TableInfo.Column("contactName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsContactBindings.put("playlistId", TableInfo.Column("playlistId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysContactBindings: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesContactBindings: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoContactBindings: TableInfo = TableInfo("contact_bindings", _columnsContactBindings, _foreignKeysContactBindings, _indicesContactBindings)
        val _existingContactBindings: TableInfo = read(connection, "contact_bindings")
        if (!_infoContactBindings.equals(_existingContactBindings)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |contact_bindings(com.smaarig.glyphbarcomposer.data.ContactBinding).
              | Expected:
              |""".trimMargin() + _infoContactBindings + """
              |
              | Found:
              |""".trimMargin() + _existingContactBindings)
        }
        val _columnsNotificationHooks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsNotificationHooks.put("id", TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("packageName", TableInfo.Column("packageName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("appName", TableInfo.Column("appName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("playlistId", TableInfo.Column("playlistId", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("presetName", TableInfo.Column("presetName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("isEnabled", TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("isProgressSync", TableInfo.Column("isProgressSync", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("notificationType", TableInfo.Column("notificationType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("notificationChannelId", TableInfo.Column("notificationChannelId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("notificationChannelName", TableInfo.Column("notificationChannelName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsNotificationHooks.put("extraData", TableInfo.Column("extraData", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysNotificationHooks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        _foreignKeysNotificationHooks.add(TableInfo.ForeignKey("playlists", "CASCADE", "NO ACTION", listOf("playlistId"), listOf("id")))
        val _indicesNotificationHooks: MutableSet<TableInfo.Index> = mutableSetOf()
        _indicesNotificationHooks.add(TableInfo.Index("index_notification_hooks_playlistId", false, listOf("playlistId"), listOf("ASC")))
        _indicesNotificationHooks.add(TableInfo.Index("index_notification_hooks_packageName", false, listOf("packageName"), listOf("ASC")))
        val _infoNotificationHooks: TableInfo = TableInfo("notification_hooks", _columnsNotificationHooks, _foreignKeysNotificationHooks, _indicesNotificationHooks)
        val _existingNotificationHooks: TableInfo = read(connection, "notification_hooks")
        if (!_infoNotificationHooks.equals(_existingNotificationHooks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |notification_hooks(com.smaarig.glyphbarcomposer.data.NotificationHook).
              | Expected:
              |""".trimMargin() + _infoNotificationHooks + """
              |
              | Found:
              |""".trimMargin() + _existingNotificationHooks)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "playlists", "sequence_steps", "event_bindings", "music_studio_projects", "music_studio_events", "contact_bindings", "notification_hooks")
  }

  public override fun clearAllTables() {
    super.performClear(true, "playlists", "sequence_steps", "event_bindings", "music_studio_projects", "music_studio_events", "contact_bindings", "notification_hooks")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(PlaylistDao::class, PlaylistDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun playlistDao(): PlaylistDao = _playlistDao.value
}
