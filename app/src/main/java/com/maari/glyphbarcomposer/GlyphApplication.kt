package com.maari.glyphbarcomposer

import android.app.Application
import com.maari.glyphbarcomposer.data.AppDatabase
import com.maari.glyphbarcomposer.repository.GlyphRepository

class GlyphApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GlyphRepository(database.playlistDao()) }
}
