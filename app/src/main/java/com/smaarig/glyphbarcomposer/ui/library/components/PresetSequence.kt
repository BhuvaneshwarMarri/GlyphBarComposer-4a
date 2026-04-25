package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.smaarig.glyphbarcomposer.model.GlyphSequence

private val ch = listOf(
    com.nothing.ketchum.Glyph.Code_25111.A_1,
    com.nothing.ketchum.Glyph.Code_25111.A_2,
    com.nothing.ketchum.Glyph.Code_25111.A_3,
    com.nothing.ketchum.Glyph.Code_25111.A_4,
    com.nothing.ketchum.Glyph.Code_25111.A_5,
    com.nothing.ketchum.Glyph.Code_25111.A_6,
    com.nothing.ketchum.Glyph.Code_22111.E1
)

data class PresetSequence(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val steps: List<GlyphSequence>
)

val presetSequences = listOf(
    PresetSequence("Pulse", "Steady rhythmic breathing", Icons.Default.Favorite, List(4) { i ->
        GlyphSequence(ch.associateWith { if (i % 2 == 0) 3 else 0 }, 500)
    }),
    PresetSequence("Wave", "Smooth horizontal sweep", Icons.Default.Waves, List(12) { i ->
        val active = if (i < 6) i else 11 - i
        GlyphSequence(mapOf(ch[active.coerceIn(0, 6)] to 3), 80)
    }),
    PresetSequence("Strobe", "High intensity flashing", Icons.Default.FlashOn, List(2) { i ->
        GlyphSequence(ch.associateWith { if (i == 0) 3 else 0 }, 100)
    }),
    PresetSequence("Knight Rider", "Back and forth pulse", Icons.AutoMirrored.Filled.DirectionsRun, List(10) { i ->
        val active = if (i < 6) i else 10 - i
        GlyphSequence(mapOf(ch[active.coerceIn(0, 5)] to 3), 100)
    }),
    PresetSequence("Fire", "Warm flickering glow", Icons.Default.Whatshot, List(8) {
        val intensities = ch.associateWith { (1..3).random() }
        GlyphSequence(intensities, (80..150).random())
    }),
    PresetSequence("Police", "Emergency response signal", Icons.Default.Warning, List(4) { i ->
        val map = if (i < 2) mapOf(ch[0] to 3, ch[1] to 3, ch[2] to 3) else mapOf(ch[3] to 3, ch[4] to 3, ch[5] to 3)
        GlyphSequence(map, 150)
    }),
    PresetSequence("Heartbeat", "Double rhythmic thump", Icons.Default.MonitorHeart, listOf(
        GlyphSequence(ch.associateWith { 3 }, 150),
        GlyphSequence(ch.associateWith { 0 }, 100),
        GlyphSequence(ch.associateWith { 2 }, 150),
        GlyphSequence(ch.associateWith { 0 }, 600)
    )),
    PresetSequence("Matrix", "Digital rain descent", Icons.Default.Code, List(14) { i ->
        val active = i % 7
        GlyphSequence(mapOf(ch[active] to 3), 120)
    })
)
