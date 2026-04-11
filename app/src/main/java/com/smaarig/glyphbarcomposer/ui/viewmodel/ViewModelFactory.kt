package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.smaarig.glyphbarcomposer.GlyphApplication
import com.smaarig.glyphbarcomposer.repository.GlyphRepository

class GlyphViewModelFactory(
    private val application: Application,
    private val repository: GlyphRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(ComposerViewModel::class.java) -> {
                ComposerViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(MusicSyncViewModel::class.java) -> {
                MusicSyncViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(LibraryViewModel::class.java) -> {
                LibraryViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(PatternLabViewModel::class.java) -> {
                PatternLabViewModel(application, repository) as T
            }
            modelClass.isAssignableFrom(RedGlyphViewModel::class.java) -> {
                RedGlyphViewModel(application) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
