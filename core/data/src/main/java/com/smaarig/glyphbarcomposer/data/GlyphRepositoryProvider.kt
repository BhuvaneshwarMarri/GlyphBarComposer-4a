package com.smaarig.glyphbarcomposer.data

import com.smaarig.glyphbarcomposer.repository.GlyphRepository

interface GlyphRepositoryProvider {
    val repository: GlyphRepository
}
