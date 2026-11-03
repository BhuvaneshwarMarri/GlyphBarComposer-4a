package com.maari.glyphbarcomposer

import android.app.Application
import com.maari.glyphbarcomposer.controller.GlyphController
import com.maari.glyphbarcomposer.data.AppDatabase
import com.maari.glyphbarcomposer.repository.GlyphRepository

class GlyphApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { GlyphRepository(database.playlistDao()) }

    override fun onCreate() {
        super.onCreate()
        // Pre-warm GlyphController to start service binding as early as possible.
        // This significantly reduces latency for cold-start widget interactions.
        GlyphController.getInstance(this)
    }
}
