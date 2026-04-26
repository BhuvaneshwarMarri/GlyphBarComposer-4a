package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.smaarig.glyphbarcomposer.controller.GlyphController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RedGlyphViewModel(application: Application) : AndroidViewModel(application) {
    private val glyphController = GlyphController.getInstance(application)
    
    private val _isRedOn = MutableStateFlow(false)
    val isRedOn = _isRedOn.asStateFlow()

    fun toggleRed() {
        _isRedOn.update { !it }
        val state = if (_isRedOn.value) 3 else 0
        glyphController.setRedGlyph(state)
    }
    
    fun setRed(on: Boolean) {
        _isRedOn.value = on
        glyphController.setRedGlyph(if (on) 3 else 0)
    }
}
