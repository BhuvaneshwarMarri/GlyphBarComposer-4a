package com.smaarig.glyphbarcomposer.`data`

import androidx.collection.LongSparseArray
import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.appendPlaceholders
import androidx.room.util.getColumnIndex
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performInTransactionSuspending
import androidx.room.util.performSuspending
import androidx.room.util.recursiveFetchLongSparseArray
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlin.text.StringBuilder
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class PlaylistDao_Impl(
  __db: RoomDatabase,
) : PlaylistDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPlaylist: EntityInsertAdapter<Playlist>

  private val __insertAdapterOfSequenceStep: EntityInsertAdapter<SequenceStep>

  private val __converters: Converters = Converters()

  private val __insertAdapterOfEventBinding: EntityInsertAdapter<EventBinding>

  private val __insertAdapterOfMusicStudioProject: EntityInsertAdapter<MusicStudioProject>

  private val __insertAdapterOfMusicStudioEvent: EntityInsertAdapter<MusicStudioEvent>

  private val __insertAdapterOfContactBinding: EntityInsertAdapter<ContactBinding>

  private val __insertAdapterOfNotificationHook: EntityInsertAdapter<NotificationHook>

  private val __deleteAdapterOfPlaylist: EntityDeleteOrUpdateAdapter<Playlist>

  private val __deleteAdapterOfEventBinding: EntityDeleteOrUpdateAdapter<EventBinding>

  private val __deleteAdapterOfMusicStudioProject: EntityDeleteOrUpdateAdapter<MusicStudioProject>

  private val __deleteAdapterOfContactBinding: EntityDeleteOrUpdateAdapter<ContactBinding>

  private val __deleteAdapterOfNotificationHook: EntityDeleteOrUpdateAdapter<NotificationHook>

  private val __updateAdapterOfMusicStudioProject: EntityDeleteOrUpdateAdapter<MusicStudioProject>
  init {
    this.__db = __db
    this.__insertAdapterOfPlaylist = object : EntityInsertAdapter<Playlist>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `playlists` (`id`,`name`,`isProgressSequence`) VALUES (nullif(?, 0),?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Playlist) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        val _tmp: Int = if (entity.isProgressSequence) 1 else 0
        statement.bindLong(3, _tmp.toLong())
      }
    }
    this.__insertAdapterOfSequenceStep = object : EntityInsertAdapter<SequenceStep>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `sequence_steps` (`stepId`,`playlistId`,`stepIndex`,`channelIntensities`,`durationMs`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: SequenceStep) {
        statement.bindLong(1, entity.stepId)
        statement.bindLong(2, entity.playlistId)
        statement.bindLong(3, entity.stepIndex.toLong())
        val _tmp: String = __converters.fromMap(entity.channelIntensities)
        statement.bindText(4, _tmp)
        statement.bindLong(5, entity.durationMs.toLong())
      }
    }
    this.__insertAdapterOfEventBinding = object : EntityInsertAdapter<EventBinding>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `event_bindings` (`eventId`,`playlistId`,`isEnabled`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: EventBinding) {
        statement.bindText(1, entity.eventId)
        statement.bindLong(2, entity.playlistId)
        val _tmp: Int = if (entity.isEnabled) 1 else 0
        statement.bindLong(3, _tmp.toLong())
      }
    }
    this.__insertAdapterOfMusicStudioProject = object : EntityInsertAdapter<MusicStudioProject>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `music_studio_projects` (`id`,`name`,`localAudioPath`,`localGlyphPath`) VALUES (nullif(?, 0),?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MusicStudioProject) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.localAudioPath)
        val _tmpLocalGlyphPath: String? = entity.localGlyphPath
        if (_tmpLocalGlyphPath == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpLocalGlyphPath)
        }
      }
    }
    this.__insertAdapterOfMusicStudioEvent = object : EntityInsertAdapter<MusicStudioEvent>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `music_studio_events` (`id`,`projectId`,`timestampMs`,`channelIntensities`,`durationMs`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MusicStudioEvent) {
        statement.bindLong(1, entity.id)
        statement.bindLong(2, entity.projectId)
        statement.bindLong(3, entity.timestampMs)
        val _tmp: String = __converters.fromMap(entity.channelIntensities)
        statement.bindText(4, _tmp)
        statement.bindLong(5, entity.durationMs.toLong())
      }
    }
    this.__insertAdapterOfContactBinding = object : EntityInsertAdapter<ContactBinding>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `contact_bindings` (`contactId`,`contactName`,`playlistId`) VALUES (?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: ContactBinding) {
        statement.bindText(1, entity.contactId)
        statement.bindText(2, entity.contactName)
        statement.bindLong(3, entity.playlistId)
      }
    }
    this.__insertAdapterOfNotificationHook = object : EntityInsertAdapter<NotificationHook>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `notification_hooks` (`id`,`packageName`,`appName`,`playlistId`,`presetName`,`isEnabled`,`isProgressSync`,`notificationType`,`notificationChannelId`,`notificationChannelName`,`extraData`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: NotificationHook) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.packageName)
        statement.bindText(3, entity.appName)
        val _tmpPlaylistId: Long? = entity.playlistId
        if (_tmpPlaylistId == null) {
          statement.bindNull(4)
        } else {
          statement.bindLong(4, _tmpPlaylistId)
        }
        val _tmpPresetName: String? = entity.presetName
        if (_tmpPresetName == null) {
          statement.bindNull(5)
        } else {
          statement.bindText(5, _tmpPresetName)
        }
        val _tmp: Int = if (entity.isEnabled) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        val _tmp_1: Int = if (entity.isProgressSync) 1 else 0
        statement.bindLong(7, _tmp_1.toLong())
        statement.bindText(8, entity.notificationType)
        val _tmpNotificationChannelId: String? = entity.notificationChannelId
        if (_tmpNotificationChannelId == null) {
          statement.bindNull(9)
        } else {
          statement.bindText(9, _tmpNotificationChannelId)
        }
        val _tmpNotificationChannelName: String? = entity.notificationChannelName
        if (_tmpNotificationChannelName == null) {
          statement.bindNull(10)
        } else {
          statement.bindText(10, _tmpNotificationChannelName)
        }
        val _tmpExtraData: String? = entity.extraData
        if (_tmpExtraData == null) {
          statement.bindNull(11)
        } else {
          statement.bindText(11, _tmpExtraData)
        }
      }
    }
    this.__deleteAdapterOfPlaylist = object : EntityDeleteOrUpdateAdapter<Playlist>() {
      protected override fun createQuery(): String = "DELETE FROM `playlists` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Playlist) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__deleteAdapterOfEventBinding = object : EntityDeleteOrUpdateAdapter<EventBinding>() {
      protected override fun createQuery(): String = "DELETE FROM `event_bindings` WHERE `eventId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: EventBinding) {
        statement.bindText(1, entity.eventId)
      }
    }
    this.__deleteAdapterOfMusicStudioProject = object : EntityDeleteOrUpdateAdapter<MusicStudioProject>() {
      protected override fun createQuery(): String = "DELETE FROM `music_studio_projects` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MusicStudioProject) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__deleteAdapterOfContactBinding = object : EntityDeleteOrUpdateAdapter<ContactBinding>() {
      protected override fun createQuery(): String = "DELETE FROM `contact_bindings` WHERE `contactId` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: ContactBinding) {
        statement.bindText(1, entity.contactId)
      }
    }
    this.__deleteAdapterOfNotificationHook = object : EntityDeleteOrUpdateAdapter<NotificationHook>() {
      protected override fun createQuery(): String = "DELETE FROM `notification_hooks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: NotificationHook) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfMusicStudioProject = object : EntityDeleteOrUpdateAdapter<MusicStudioProject>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `music_studio_projects` SET `id` = ?,`name` = ?,`localAudioPath` = ?,`localGlyphPath` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MusicStudioProject) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.localAudioPath)
        val _tmpLocalGlyphPath: String? = entity.localGlyphPath
        if (_tmpLocalGlyphPath == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpLocalGlyphPath)
        }
        statement.bindLong(5, entity.id)
      }
    }
  }

  public override suspend fun insertPlaylist(playlist: Playlist): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfPlaylist.insertAndReturnId(_connection, playlist)
    _result
  }

  public override suspend fun insertSteps(steps: List<SequenceStep>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfSequenceStep.insert(_connection, steps)
  }

  public override suspend fun insertEventBinding(binding: EventBinding): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfEventBinding.insert(_connection, binding)
  }

  public override suspend fun insertMusicProject(project: MusicStudioProject): Long = performSuspending(__db, false, true) { _connection ->
    val _result: Long = __insertAdapterOfMusicStudioProject.insertAndReturnId(_connection, project)
    _result
  }

  public override suspend fun insertMusicEvents(events: List<MusicStudioEvent>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfMusicStudioEvent.insert(_connection, events)
  }

  public override suspend fun insertContactBinding(binding: ContactBinding): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfContactBinding.insert(_connection, binding)
  }

  public override suspend fun insertNotificationHook(hook: NotificationHook): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfNotificationHook.insert(_connection, hook)
  }

  public override suspend fun deletePlaylist(playlist: Playlist): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfPlaylist.handle(_connection, playlist)
  }

  public override suspend fun deleteEventBinding(binding: EventBinding): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfEventBinding.handle(_connection, binding)
  }

  public override suspend fun deleteMusicProject(project: MusicStudioProject): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfMusicStudioProject.handle(_connection, project)
  }

  public override suspend fun deleteContactBinding(binding: ContactBinding): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfContactBinding.handle(_connection, binding)
  }

  public override suspend fun deleteNotificationHook(hook: NotificationHook): Unit = performSuspending(__db, false, true) { _connection ->
    __deleteAdapterOfNotificationHook.handle(_connection, hook)
  }

  public override suspend fun updateMusicProject(project: MusicStudioProject): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfMusicStudioProject.handle(_connection, project)
  }

  public override suspend fun replacePlaylistSteps(playlist: Playlist, steps: List<SequenceStep>): Unit = performInTransactionSuspending(__db) {
    super@PlaylistDao_Impl.replacePlaylistSteps(playlist, steps)
  }

  public override suspend fun replaceMusicProjectEvents(project: MusicStudioProject, events: List<MusicStudioEvent>): Unit = performInTransactionSuspending(__db) {
    super@PlaylistDao_Impl.replaceMusicProjectEvents(project, events)
  }

  public override fun getAllPlaylists(): Flow<List<PlaylistWithSteps>> {
    val _sql: String = "SELECT * FROM playlists"
    return createFlow(__db, true, arrayOf("sequence_steps", "playlists")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsProgressSequence: Int = getColumnIndexOrThrow(_stmt, "isProgressSequence")
        val _collectionSteps: LongSparseArray<MutableList<SequenceStep>> = LongSparseArray<MutableList<SequenceStep>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionSteps.containsKey(_tmpKey)) {
            _collectionSteps.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipsequenceStepsAscomSmaarigGlyphbarcomposerDataSequenceStep(_connection, _collectionSteps)
        val _result: MutableList<PlaylistWithSteps> = mutableListOf()
        while (_stmt.step()) {
          val _item: PlaylistWithSteps
          val _tmpPlaylist: Playlist
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsProgressSequence: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProgressSequence).toInt()
          _tmpIsProgressSequence = _tmp != 0
          _tmpPlaylist = Playlist(_tmpId,_tmpName,_tmpIsProgressSequence)
          val _tmpStepsCollection: MutableList<SequenceStep>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpStepsCollection = checkNotNull(_collectionSteps.get(_tmpKey_1))
          _item = PlaylistWithSteps(_tmpPlaylist,_tmpStepsCollection)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllEventBindings(): Flow<List<EventBindingWithPlaylist>> {
    val _sql: String = "SELECT * FROM event_bindings"
    return createFlow(__db, true, arrayOf("playlists", "event_bindings")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfEventId: Int = getColumnIndexOrThrow(_stmt, "eventId")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          _collectionPlaylist.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist(_connection, _collectionPlaylist)
        val _result: MutableList<EventBindingWithPlaylist> = mutableListOf()
        while (_stmt.step()) {
          val _item: EventBindingWithPlaylist
          val _tmpBinding: EventBinding
          val _tmpEventId: String
          _tmpEventId = _stmt.getText(_columnIndexOfEventId)
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          _tmpBinding = EventBinding(_tmpEventId,_tmpPlaylistId,_tmpIsEnabled)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          _item = EventBindingWithPlaylist(_tmpBinding,_tmpPlaylist)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllMusicProjects(): Flow<List<MusicProjectWithEvents>> {
    val _sql: String = "SELECT * FROM music_studio_projects"
    return createFlow(__db, true, arrayOf("music_studio_events", "music_studio_projects")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLocalAudioPath: Int = getColumnIndexOrThrow(_stmt, "localAudioPath")
        val _columnIndexOfLocalGlyphPath: Int = getColumnIndexOrThrow(_stmt, "localGlyphPath")
        val _collectionEvents: LongSparseArray<MutableList<MusicStudioEvent>> = LongSparseArray<MutableList<MusicStudioEvent>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionEvents.containsKey(_tmpKey)) {
            _collectionEvents.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipmusicStudioEventsAscomSmaarigGlyphbarcomposerDataMusicStudioEvent(_connection, _collectionEvents)
        val _result: MutableList<MusicProjectWithEvents> = mutableListOf()
        while (_stmt.step()) {
          val _item: MusicProjectWithEvents
          val _tmpProject: MusicStudioProject
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLocalAudioPath: String
          _tmpLocalAudioPath = _stmt.getText(_columnIndexOfLocalAudioPath)
          val _tmpLocalGlyphPath: String?
          if (_stmt.isNull(_columnIndexOfLocalGlyphPath)) {
            _tmpLocalGlyphPath = null
          } else {
            _tmpLocalGlyphPath = _stmt.getText(_columnIndexOfLocalGlyphPath)
          }
          _tmpProject = MusicStudioProject(_tmpId,_tmpName,_tmpLocalAudioPath,_tmpLocalGlyphPath)
          val _tmpEventsCollection: MutableList<MusicStudioEvent>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpEventsCollection = checkNotNull(_collectionEvents.get(_tmpKey_1))
          _item = MusicProjectWithEvents(_tmpProject,_tmpEventsCollection)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getMusicProjectById(projectId: Long): MusicStudioProject? {
    val _sql: String = "SELECT * FROM music_studio_projects WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, projectId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfLocalAudioPath: Int = getColumnIndexOrThrow(_stmt, "localAudioPath")
        val _columnIndexOfLocalGlyphPath: Int = getColumnIndexOrThrow(_stmt, "localGlyphPath")
        val _result: MusicStudioProject?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpLocalAudioPath: String
          _tmpLocalAudioPath = _stmt.getText(_columnIndexOfLocalAudioPath)
          val _tmpLocalGlyphPath: String?
          if (_stmt.isNull(_columnIndexOfLocalGlyphPath)) {
            _tmpLocalGlyphPath = null
          } else {
            _tmpLocalGlyphPath = _stmt.getText(_columnIndexOfLocalGlyphPath)
          }
          _result = MusicStudioProject(_tmpId,_tmpName,_tmpLocalAudioPath,_tmpLocalGlyphPath)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllContactBindings(): Flow<List<ContactBindingWithPlaylist>> {
    val _sql: String = "SELECT * FROM contact_bindings"
    return createFlow(__db, true, arrayOf("playlists", "contact_bindings")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfContactId: Int = getColumnIndexOrThrow(_stmt, "contactId")
        val _columnIndexOfContactName: Int = getColumnIndexOrThrow(_stmt, "contactName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          _collectionPlaylist.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist_1(_connection, _collectionPlaylist)
        val _result: MutableList<ContactBindingWithPlaylist> = mutableListOf()
        while (_stmt.step()) {
          val _item: ContactBindingWithPlaylist
          val _tmpBinding: ContactBinding
          val _tmpContactId: String
          _tmpContactId = _stmt.getText(_columnIndexOfContactId)
          val _tmpContactName: String
          _tmpContactName = _stmt.getText(_columnIndexOfContactName)
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpBinding = ContactBinding(_tmpContactId,_tmpContactName,_tmpPlaylistId)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          if (_tmpPlaylist == null) {
            error("Relationship item 'playlist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'playlistId' and entityColumn named 'id'.")
          }
          _item = ContactBindingWithPlaylist(_tmpBinding,_tmpPlaylist)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getContactBindingsList(): List<ContactBindingWithPlaylist> {
    val _sql: String = "SELECT * FROM contact_bindings"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfContactId: Int = getColumnIndexOrThrow(_stmt, "contactId")
        val _columnIndexOfContactName: Int = getColumnIndexOrThrow(_stmt, "contactName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          _collectionPlaylist.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist_1(_connection, _collectionPlaylist)
        val _result: MutableList<ContactBindingWithPlaylist> = mutableListOf()
        while (_stmt.step()) {
          val _item: ContactBindingWithPlaylist
          val _tmpBinding: ContactBinding
          val _tmpContactId: String
          _tmpContactId = _stmt.getText(_columnIndexOfContactId)
          val _tmpContactName: String
          _tmpContactName = _stmt.getText(_columnIndexOfContactName)
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpBinding = ContactBinding(_tmpContactId,_tmpContactName,_tmpPlaylistId)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          if (_tmpPlaylist == null) {
            error("Relationship item 'playlist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'playlistId' and entityColumn named 'id'.")
          }
          _item = ContactBindingWithPlaylist(_tmpBinding,_tmpPlaylist)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getContactBinding(contactId: String): ContactBindingWithPlaylist? {
    val _sql: String = "SELECT * FROM contact_bindings WHERE contactId = ? LIMIT 1"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, contactId)
        val _columnIndexOfContactId: Int = getColumnIndexOrThrow(_stmt, "contactId")
        val _columnIndexOfContactName: Int = getColumnIndexOrThrow(_stmt, "contactName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          _collectionPlaylist.put(_tmpKey, null)
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist_1(_connection, _collectionPlaylist)
        val _result: ContactBindingWithPlaylist?
        if (_stmt.step()) {
          val _tmpBinding: ContactBinding
          val _tmpContactId: String
          _tmpContactId = _stmt.getText(_columnIndexOfContactId)
          val _tmpContactName: String
          _tmpContactName = _stmt.getText(_columnIndexOfContactName)
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpBinding = ContactBinding(_tmpContactId,_tmpContactName,_tmpPlaylistId)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          if (_tmpPlaylist == null) {
            error("Relationship item 'playlist' was expected to be NON-NULL but is NULL in @Relation involving a parent column named 'playlistId' and entityColumn named 'id'.")
          }
          _result = ContactBindingWithPlaylist(_tmpBinding,_tmpPlaylist)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllNotificationHooks(): Flow<List<NotificationHookWithPlaylist>> {
    val _sql: String = "SELECT * FROM notification_hooks"
    return createFlow(__db, true, arrayOf("playlists", "notification_hooks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfAppName: Int = getColumnIndexOrThrow(_stmt, "appName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _columnIndexOfPresetName: Int = getColumnIndexOrThrow(_stmt, "presetName")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsProgressSync: Int = getColumnIndexOrThrow(_stmt, "isProgressSync")
        val _columnIndexOfNotificationType: Int = getColumnIndexOrThrow(_stmt, "notificationType")
        val _columnIndexOfNotificationChannelId: Int = getColumnIndexOrThrow(_stmt, "notificationChannelId")
        val _columnIndexOfNotificationChannelName: Int = getColumnIndexOrThrow(_stmt, "notificationChannelName")
        val _columnIndexOfExtraData: Int = getColumnIndexOrThrow(_stmt, "extraData")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpKey = null
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          if (_tmpKey != null) {
            _collectionPlaylist.put(_tmpKey, null)
          }
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist(_connection, _collectionPlaylist)
        val _result: MutableList<NotificationHookWithPlaylist> = mutableListOf()
        while (_stmt.step()) {
          val _item: NotificationHookWithPlaylist
          val _tmpHook: NotificationHook
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpAppName: String
          _tmpAppName = _stmt.getText(_columnIndexOfAppName)
          val _tmpPlaylistId: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpPlaylistId = null
          } else {
            _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          val _tmpPresetName: String?
          if (_stmt.isNull(_columnIndexOfPresetName)) {
            _tmpPresetName = null
          } else {
            _tmpPresetName = _stmt.getText(_columnIndexOfPresetName)
          }
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsProgressSync: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsProgressSync).toInt()
          _tmpIsProgressSync = _tmp_1 != 0
          val _tmpNotificationType: String
          _tmpNotificationType = _stmt.getText(_columnIndexOfNotificationType)
          val _tmpNotificationChannelId: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelId)) {
            _tmpNotificationChannelId = null
          } else {
            _tmpNotificationChannelId = _stmt.getText(_columnIndexOfNotificationChannelId)
          }
          val _tmpNotificationChannelName: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelName)) {
            _tmpNotificationChannelName = null
          } else {
            _tmpNotificationChannelName = _stmt.getText(_columnIndexOfNotificationChannelName)
          }
          val _tmpExtraData: String?
          if (_stmt.isNull(_columnIndexOfExtraData)) {
            _tmpExtraData = null
          } else {
            _tmpExtraData = _stmt.getText(_columnIndexOfExtraData)
          }
          _tmpHook = NotificationHook(_tmpId,_tmpPackageName,_tmpAppName,_tmpPlaylistId,_tmpPresetName,_tmpIsEnabled,_tmpIsProgressSync,_tmpNotificationType,_tmpNotificationChannelId,_tmpNotificationChannelName,_tmpExtraData)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpKey_1 = null
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          if (_tmpKey_1 != null) {
            _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          } else {
            _tmpPlaylist = null
          }
          _item = NotificationHookWithPlaylist(_tmpHook,_tmpPlaylist)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getNotificationHooksList(): List<NotificationHookWithPlaylist> {
    val _sql: String = "SELECT * FROM notification_hooks"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfAppName: Int = getColumnIndexOrThrow(_stmt, "appName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _columnIndexOfPresetName: Int = getColumnIndexOrThrow(_stmt, "presetName")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsProgressSync: Int = getColumnIndexOrThrow(_stmt, "isProgressSync")
        val _columnIndexOfNotificationType: Int = getColumnIndexOrThrow(_stmt, "notificationType")
        val _columnIndexOfNotificationChannelId: Int = getColumnIndexOrThrow(_stmt, "notificationChannelId")
        val _columnIndexOfNotificationChannelName: Int = getColumnIndexOrThrow(_stmt, "notificationChannelName")
        val _columnIndexOfExtraData: Int = getColumnIndexOrThrow(_stmt, "extraData")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpKey = null
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          if (_tmpKey != null) {
            _collectionPlaylist.put(_tmpKey, null)
          }
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist(_connection, _collectionPlaylist)
        val _result: MutableList<NotificationHookWithPlaylist> = mutableListOf()
        while (_stmt.step()) {
          val _item: NotificationHookWithPlaylist
          val _tmpHook: NotificationHook
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpAppName: String
          _tmpAppName = _stmt.getText(_columnIndexOfAppName)
          val _tmpPlaylistId: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpPlaylistId = null
          } else {
            _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          val _tmpPresetName: String?
          if (_stmt.isNull(_columnIndexOfPresetName)) {
            _tmpPresetName = null
          } else {
            _tmpPresetName = _stmt.getText(_columnIndexOfPresetName)
          }
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsProgressSync: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsProgressSync).toInt()
          _tmpIsProgressSync = _tmp_1 != 0
          val _tmpNotificationType: String
          _tmpNotificationType = _stmt.getText(_columnIndexOfNotificationType)
          val _tmpNotificationChannelId: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelId)) {
            _tmpNotificationChannelId = null
          } else {
            _tmpNotificationChannelId = _stmt.getText(_columnIndexOfNotificationChannelId)
          }
          val _tmpNotificationChannelName: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelName)) {
            _tmpNotificationChannelName = null
          } else {
            _tmpNotificationChannelName = _stmt.getText(_columnIndexOfNotificationChannelName)
          }
          val _tmpExtraData: String?
          if (_stmt.isNull(_columnIndexOfExtraData)) {
            _tmpExtraData = null
          } else {
            _tmpExtraData = _stmt.getText(_columnIndexOfExtraData)
          }
          _tmpHook = NotificationHook(_tmpId,_tmpPackageName,_tmpAppName,_tmpPlaylistId,_tmpPresetName,_tmpIsEnabled,_tmpIsProgressSync,_tmpNotificationType,_tmpNotificationChannelId,_tmpNotificationChannelName,_tmpExtraData)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpKey_1 = null
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          if (_tmpKey_1 != null) {
            _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          } else {
            _tmpPlaylist = null
          }
          _item = NotificationHookWithPlaylist(_tmpHook,_tmpPlaylist)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getHooksForPackage(packageName: String): List<NotificationHookWithPlaylist> {
    val _sql: String = "SELECT * FROM notification_hooks WHERE packageName = ? AND isEnabled = 1"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, packageName)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfAppName: Int = getColumnIndexOrThrow(_stmt, "appName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _columnIndexOfPresetName: Int = getColumnIndexOrThrow(_stmt, "presetName")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsProgressSync: Int = getColumnIndexOrThrow(_stmt, "isProgressSync")
        val _columnIndexOfNotificationType: Int = getColumnIndexOrThrow(_stmt, "notificationType")
        val _columnIndexOfNotificationChannelId: Int = getColumnIndexOrThrow(_stmt, "notificationChannelId")
        val _columnIndexOfNotificationChannelName: Int = getColumnIndexOrThrow(_stmt, "notificationChannelName")
        val _columnIndexOfExtraData: Int = getColumnIndexOrThrow(_stmt, "extraData")
        val _collectionPlaylist: LongSparseArray<Playlist?> = LongSparseArray<Playlist?>()
        while (_stmt.step()) {
          val _tmpKey: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpKey = null
          } else {
            _tmpKey = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          if (_tmpKey != null) {
            _collectionPlaylist.put(_tmpKey, null)
          }
        }
        _stmt.reset()
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist(_connection, _collectionPlaylist)
        val _result: MutableList<NotificationHookWithPlaylist> = mutableListOf()
        while (_stmt.step()) {
          val _item: NotificationHookWithPlaylist
          val _tmpHook: NotificationHook
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpAppName: String
          _tmpAppName = _stmt.getText(_columnIndexOfAppName)
          val _tmpPlaylistId: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpPlaylistId = null
          } else {
            _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          val _tmpPresetName: String?
          if (_stmt.isNull(_columnIndexOfPresetName)) {
            _tmpPresetName = null
          } else {
            _tmpPresetName = _stmt.getText(_columnIndexOfPresetName)
          }
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsProgressSync: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsProgressSync).toInt()
          _tmpIsProgressSync = _tmp_1 != 0
          val _tmpNotificationType: String
          _tmpNotificationType = _stmt.getText(_columnIndexOfNotificationType)
          val _tmpNotificationChannelId: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelId)) {
            _tmpNotificationChannelId = null
          } else {
            _tmpNotificationChannelId = _stmt.getText(_columnIndexOfNotificationChannelId)
          }
          val _tmpNotificationChannelName: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelName)) {
            _tmpNotificationChannelName = null
          } else {
            _tmpNotificationChannelName = _stmt.getText(_columnIndexOfNotificationChannelName)
          }
          val _tmpExtraData: String?
          if (_stmt.isNull(_columnIndexOfExtraData)) {
            _tmpExtraData = null
          } else {
            _tmpExtraData = _stmt.getText(_columnIndexOfExtraData)
          }
          _tmpHook = NotificationHook(_tmpId,_tmpPackageName,_tmpAppName,_tmpPlaylistId,_tmpPresetName,_tmpIsEnabled,_tmpIsProgressSync,_tmpNotificationType,_tmpNotificationChannelId,_tmpNotificationChannelName,_tmpExtraData)
          val _tmpPlaylist: Playlist?
          val _tmpKey_1: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpKey_1 = null
          } else {
            _tmpKey_1 = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          if (_tmpKey_1 != null) {
            _tmpPlaylist = _collectionPlaylist.get(_tmpKey_1)
          } else {
            _tmpPlaylist = null
          }
          _item = NotificationHookWithPlaylist(_tmpHook,_tmpPlaylist)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getNotificationHookById(hookId: Long): NotificationHook? {
    val _sql: String = "SELECT * FROM notification_hooks WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, hookId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfPackageName: Int = getColumnIndexOrThrow(_stmt, "packageName")
        val _columnIndexOfAppName: Int = getColumnIndexOrThrow(_stmt, "appName")
        val _columnIndexOfPlaylistId: Int = getColumnIndexOrThrow(_stmt, "playlistId")
        val _columnIndexOfPresetName: Int = getColumnIndexOrThrow(_stmt, "presetName")
        val _columnIndexOfIsEnabled: Int = getColumnIndexOrThrow(_stmt, "isEnabled")
        val _columnIndexOfIsProgressSync: Int = getColumnIndexOrThrow(_stmt, "isProgressSync")
        val _columnIndexOfNotificationType: Int = getColumnIndexOrThrow(_stmt, "notificationType")
        val _columnIndexOfNotificationChannelId: Int = getColumnIndexOrThrow(_stmt, "notificationChannelId")
        val _columnIndexOfNotificationChannelName: Int = getColumnIndexOrThrow(_stmt, "notificationChannelName")
        val _columnIndexOfExtraData: Int = getColumnIndexOrThrow(_stmt, "extraData")
        val _result: NotificationHook?
        if (_stmt.step()) {
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpPackageName: String
          _tmpPackageName = _stmt.getText(_columnIndexOfPackageName)
          val _tmpAppName: String
          _tmpAppName = _stmt.getText(_columnIndexOfAppName)
          val _tmpPlaylistId: Long?
          if (_stmt.isNull(_columnIndexOfPlaylistId)) {
            _tmpPlaylistId = null
          } else {
            _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          }
          val _tmpPresetName: String?
          if (_stmt.isNull(_columnIndexOfPresetName)) {
            _tmpPresetName = null
          } else {
            _tmpPresetName = _stmt.getText(_columnIndexOfPresetName)
          }
          val _tmpIsEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsEnabled).toInt()
          _tmpIsEnabled = _tmp != 0
          val _tmpIsProgressSync: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsProgressSync).toInt()
          _tmpIsProgressSync = _tmp_1 != 0
          val _tmpNotificationType: String
          _tmpNotificationType = _stmt.getText(_columnIndexOfNotificationType)
          val _tmpNotificationChannelId: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelId)) {
            _tmpNotificationChannelId = null
          } else {
            _tmpNotificationChannelId = _stmt.getText(_columnIndexOfNotificationChannelId)
          }
          val _tmpNotificationChannelName: String?
          if (_stmt.isNull(_columnIndexOfNotificationChannelName)) {
            _tmpNotificationChannelName = null
          } else {
            _tmpNotificationChannelName = _stmt.getText(_columnIndexOfNotificationChannelName)
          }
          val _tmpExtraData: String?
          if (_stmt.isNull(_columnIndexOfExtraData)) {
            _tmpExtraData = null
          } else {
            _tmpExtraData = _stmt.getText(_columnIndexOfExtraData)
          }
          _result = NotificationHook(_tmpId,_tmpPackageName,_tmpAppName,_tmpPlaylistId,_tmpPresetName,_tmpIsEnabled,_tmpIsProgressSync,_tmpNotificationType,_tmpNotificationChannelId,_tmpNotificationChannelName,_tmpExtraData)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getPlaylistWithSteps(playlistId: Long): PlaylistWithSteps? {
    val _sql: String = "SELECT * FROM playlists WHERE id = ? LIMIT 1"
    return performSuspending(__db, true, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, playlistId)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfIsProgressSequence: Int = getColumnIndexOrThrow(_stmt, "isProgressSequence")
        val _collectionSteps: LongSparseArray<MutableList<SequenceStep>> = LongSparseArray<MutableList<SequenceStep>>()
        while (_stmt.step()) {
          val _tmpKey: Long
          _tmpKey = _stmt.getLong(_columnIndexOfId)
          if (!_collectionSteps.containsKey(_tmpKey)) {
            _collectionSteps.put(_tmpKey, mutableListOf())
          }
        }
        _stmt.reset()
        __fetchRelationshipsequenceStepsAscomSmaarigGlyphbarcomposerDataSequenceStep(_connection, _collectionSteps)
        val _result: PlaylistWithSteps?
        if (_stmt.step()) {
          val _tmpPlaylist: Playlist
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsProgressSequence: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProgressSequence).toInt()
          _tmpIsProgressSequence = _tmp != 0
          _tmpPlaylist = Playlist(_tmpId,_tmpName,_tmpIsProgressSequence)
          val _tmpStepsCollection: MutableList<SequenceStep>
          val _tmpKey_1: Long
          _tmpKey_1 = _stmt.getLong(_columnIndexOfId)
          _tmpStepsCollection = checkNotNull(_collectionSteps.get(_tmpKey_1))
          _result = PlaylistWithSteps(_tmpPlaylist,_tmpStepsCollection)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteStepsForPlaylist(playlistId: Long) {
    val _sql: String = "DELETE FROM sequence_steps WHERE playlistId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, playlistId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteEventsForProject(projectId: Long) {
    val _sql: String = "DELETE FROM music_studio_events WHERE projectId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, projectId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  private fun __fetchRelationshipsequenceStepsAscomSmaarigGlyphbarcomposerDataSequenceStep(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<SequenceStep>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshipsequenceStepsAscomSmaarigGlyphbarcomposerDataSequenceStep(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `stepId`,`playlistId`,`stepIndex`,`channelIntensities`,`durationMs` FROM `sequence_steps` WHERE `playlistId` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "playlistId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfStepId: Int = 0
      val _columnIndexOfPlaylistId: Int = 1
      val _columnIndexOfStepIndex: Int = 2
      val _columnIndexOfChannelIntensities: Int = 3
      val _columnIndexOfDurationMs: Int = 4
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<SequenceStep>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: SequenceStep
          val _tmpStepId: Long
          _tmpStepId = _stmt.getLong(_columnIndexOfStepId)
          val _tmpPlaylistId: Long
          _tmpPlaylistId = _stmt.getLong(_columnIndexOfPlaylistId)
          val _tmpStepIndex: Int
          _tmpStepIndex = _stmt.getLong(_columnIndexOfStepIndex).toInt()
          val _tmpChannelIntensities: Map<Int, Int>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfChannelIntensities)
          _tmpChannelIntensities = __converters.toMap(_tmp)
          val _tmpDurationMs: Int
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs).toInt()
          _item_1 = SequenceStep(_tmpStepId,_tmpPlaylistId,_tmpStepIndex,_tmpChannelIntensities,_tmpDurationMs)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist(_connection: SQLiteConnection, _map: LongSparseArray<Playlist?>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, false) { _tmpMap ->
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`name`,`isProgressSequence` FROM `playlists` WHERE `id` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfName: Int = 1
      val _columnIndexOfIsProgressSequence: Int = 2
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: Playlist?
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsProgressSequence: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProgressSequence).toInt()
          _tmpIsProgressSequence = _tmp != 0
          _item_1 = Playlist(_tmpId,_tmpName,_tmpIsProgressSequence)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipmusicStudioEventsAscomSmaarigGlyphbarcomposerDataMusicStudioEvent(_connection: SQLiteConnection, _map: LongSparseArray<MutableList<MusicStudioEvent>>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, true) { _tmpMap ->
        __fetchRelationshipmusicStudioEventsAscomSmaarigGlyphbarcomposerDataMusicStudioEvent(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`projectId`,`timestampMs`,`channelIntensities`,`durationMs` FROM `music_studio_events` WHERE `projectId` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "projectId")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfProjectId: Int = 1
      val _columnIndexOfTimestampMs: Int = 2
      val _columnIndexOfChannelIntensities: Int = 3
      val _columnIndexOfDurationMs: Int = 4
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        val _tmpRelation: MutableList<MusicStudioEvent>? = _map.get(_tmpKey)
        if (_tmpRelation != null) {
          val _item_1: MusicStudioEvent
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpProjectId: Long
          _tmpProjectId = _stmt.getLong(_columnIndexOfProjectId)
          val _tmpTimestampMs: Long
          _tmpTimestampMs = _stmt.getLong(_columnIndexOfTimestampMs)
          val _tmpChannelIntensities: Map<Int, Int>
          val _tmp: String
          _tmp = _stmt.getText(_columnIndexOfChannelIntensities)
          _tmpChannelIntensities = __converters.toMap(_tmp)
          val _tmpDurationMs: Int
          _tmpDurationMs = _stmt.getLong(_columnIndexOfDurationMs).toInt()
          _item_1 = MusicStudioEvent(_tmpId,_tmpProjectId,_tmpTimestampMs,_tmpChannelIntensities,_tmpDurationMs)
          _tmpRelation.add(_item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  private fun __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist_1(_connection: SQLiteConnection, _map: LongSparseArray<Playlist?>) {
    if (_map.isEmpty()) {
      return
    }
    if (_map.size() > 999) {
      recursiveFetchLongSparseArray(_map, false) { _tmpMap ->
        __fetchRelationshipplaylistsAscomSmaarigGlyphbarcomposerDataPlaylist_1(_connection, _tmpMap)
      }
      return
    }
    val _stringBuilder: StringBuilder = StringBuilder()
    _stringBuilder.append("SELECT `id`,`name`,`isProgressSequence` FROM `playlists` WHERE `id` IN (")
    val _inputSize: Int = _map.size()
    appendPlaceholders(_stringBuilder, _inputSize)
    _stringBuilder.append(")")
    val _sql: String = _stringBuilder.toString()
    val _stmt: SQLiteStatement = _connection.prepare(_sql)
    var _argIndex: Int = 1
    for (i in 0 until _map.size()) {
      val _item: Long = _map.keyAt(i)
      _stmt.bindLong(_argIndex, _item)
      _argIndex++
    }
    try {
      val _itemKeyIndex: Int = getColumnIndex(_stmt, "id")
      if (_itemKeyIndex == -1) {
        return
      }
      val _columnIndexOfId: Int = 0
      val _columnIndexOfName: Int = 1
      val _columnIndexOfIsProgressSequence: Int = 2
      while (_stmt.step()) {
        val _tmpKey: Long
        _tmpKey = _stmt.getLong(_itemKeyIndex)
        if (_map.containsKey(_tmpKey)) {
          val _item_1: Playlist
          val _tmpId: Long
          _tmpId = _stmt.getLong(_columnIndexOfId)
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpIsProgressSequence: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsProgressSequence).toInt()
          _tmpIsProgressSequence = _tmp != 0
          _item_1 = Playlist(_tmpId,_tmpName,_tmpIsProgressSequence)
          _map.put(_tmpKey, _item_1)
        }
      }
    } finally {
      _stmt.close()
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
