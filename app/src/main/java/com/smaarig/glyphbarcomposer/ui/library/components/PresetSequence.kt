package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.smaarig.glyphbarcomposer.controller.GlyphConstants
import com.smaarig.glyphbarcomposer.model.GlyphSequence

private val ch = GlyphConstants.PHONE_4A_CHANNELS

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
    PresetSequence(
        "Knight Rider",
        "Classic scanner sweep",
        Icons.AutoMirrored.Filled.DirectionsRun,
        List(10) { i ->
            val active = if (i < 6) i else 10 - i
            GlyphSequence(mapOf(ch[active.coerceIn(0, 5)] to 3), 80)
        }),
    PresetSequence("Strobe", "High intensity flashing", Icons.Default.FlashOn, List(2) { i ->
        GlyphSequence(ch.associateWith { if (i == 0) 3 else 0 }, 100)
    }),
    PresetSequence("Fire", "Warm flickering glow", Icons.Default.Whatshot, List(8) {
        val intensities = ch.associateWith { (1..3).random() }
        GlyphSequence(intensities, (80..150).random())
    }),
    PresetSequence("Police", "Emergency response signal", Icons.Default.Warning, List(4) { i ->
        val map = if (i < 2) mapOf(ch[0] to 3, ch[1] to 3, ch[2] to 3) else mapOf(
            ch[3] to 3,
            ch[4] to 3,
            ch[5] to 3
        )
        GlyphSequence(map, 150)
    }),
    PresetSequence(
        "Heartbeat", "Double rhythmic thump", Icons.Default.MonitorHeart, listOf(
            GlyphSequence(ch.associateWith { 3 }, 150),
            GlyphSequence(ch.associateWith { 0 }, 100),
            GlyphSequence(ch.associateWith { 2 }, 150),
            GlyphSequence(ch.associateWith { 0 }, 600)
        )
    ),
    PresetSequence("Matrix", "Digital rain descent", Icons.Default.Code, List(7) { i ->
        GlyphSequence(mapOf(ch[i] to 3), 100)
    }),
    PresetSequence("Sparkle", "Random light points", Icons.Default.FlashOn, List(10) {
        val active = (0..6).random()
        GlyphSequence(mapOf(ch[active] to (2..3).random()), (50..120).random())
    })
)
