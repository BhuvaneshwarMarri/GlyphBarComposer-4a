package com.smaarig.glyphbarcomposer

import android.app.Application
import com.smaarig.glyphbarcomposer.data.AppDatabase
import com.smaarig.glyphbarcomposer.repository.GlyphRepository

class GlyphApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GlyphRepository(database.playlistDao()) }
}
