package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController

object WidgetKeys {
    val GlyphIndexKey = ActionParameters.Key<Int>("glyph_index")
}

fun getIntensityColor(intensity: Int): Color {
    return when (intensity) {
        0 -> Color(0xFF1C1C1C)
        1 -> Color(0xFF686868)
        2 -> Color(0xFFCDCDCD)
        3 -> Color(0xFFFFFFFF)
        4 -> Color(0xFFC62828)
        5 -> Color(0xFFEF5350)
        6 -> Color(0xFFFF1744)
        else -> Color(0xFF1C1C1C)
    }
}

class CycleIntensityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val glyphIndex = parameters[WidgetKeys.GlyphIndexKey] ?: return

        var newIntensitiesStr = "0,0,0,0,0,0,0"
        var newIntensity = 0
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val intensitiesStr = prefs[stringPreferencesKey("intensities")] ?: "0,0,0,0,0,0,0"
            val intensities = intensitiesStr.split(",").map { it.toInt() }.toMutableList()
            
            val isRed = glyphIndex == 6
            val states = if (isRed) listOf(0, 4, 5, 6) else listOf(0, 1, 2, 3)
            val currentIdx = states.indexOf(intensities[glyphIndex]).coerceAtLeast(0)
            newIntensity = states[(currentIdx + 1) % states.size]
            
            intensities[glyphIndex] = newIntensity
            newIntensitiesStr = intensities.joinToString(",")
            
            prefs.toMutablePreferences().apply {
                this[stringPreferencesKey("intensities")] = newIntensitiesStr
            }
        }
        
        // Update physical glyphs
        val glyphController = GlyphController.getInstance(context)
        val channels = listOf(
            Glyph.Code_25111.A_1,
            Glyph.Code_25111.A_2,
            Glyph.Code_25111.A_3,
            Glyph.Code_25111.A_4,
            Glyph.Code_25111.A_5,
            Glyph.Code_25111.A_6,
            Glyph.Code_22111.E1
        )
        
        val finalIntensities = newIntensitiesStr.split(",").map { it.toInt() }
        val intensityMap = channels.mapIndexed { index, ch -> ch to finalIntensities[index] }.toMap()
        glyphController.applyGlyphStateWithIntensities(intensityMap, 2000)
        
        if (glyphIndex == 6) {
            glyphController.setRedGlyph(newIntensity)
        }

        // Update all widgets
        GlyphComposerHorizontalWidget().updateAll(context)
        GlyphComposerVerticalWidget().updateAll(context)
    }
}
