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

// ─── Shared preference key ──────────────────────────────────────────────────
val INTENSITIES_KEY = stringPreferencesKey("intensities")
const val DEFAULT_INTENSITIES = "0,0,0,0,0,0,0"

// ─── Widget action parameter keys ───────────────────────────────────────────
object WidgetKeys {
    val GlyphIndexKey = ActionParameters.Key<Int>("glyph_index")
}

// ─── Intensity colour palette (matches CommonUi.intensityColor exactly) ─────
//   0 = OFF        → dark grey
//   1 = LOW white  → mid grey
//   2 = MED white  → light grey
//   3 = HIGH white → pure white
//   4 = RED low    → dark red
//   5 = RED med    → medium red
//   6 = RED full   → bright red
fun getIntensityColor(intensity: Int): Color = when (intensity) {
    0    -> Color(0xFF1C1C1C)
    1    -> Color(0xFF686868)
    2    -> Color(0xFFCDCDCD)
    3    -> Color(0xFFFFFFFF)
    4    -> Color(0xFFC62828)
    5    -> Color(0xFFEF5350)
    6    -> Color(0xFFFF1744)
    else -> Color(0xFF1C1C1C)
}

// ─── Cycle intensity states (mirrors GlyphScrollPicker / OldGlyphButton) ────
fun cycleIntensity(current: Int, isRed: Boolean): Int {
    val states = if (isRed) listOf(0, 4, 5, 6) else listOf(0, 1, 2, 3)
    val idx = states.indexOf(current).coerceAtLeast(0)
    return states[(idx + 1) % states.size]
}

// ─── Shared tap action ───────────────────────────────────────────────────────
/**
 * Fired when any glyph button in either widget is tapped.
 * Cycles intensity → persists → fires physical glyphs → redraws all widgets.
 */
class CycleIntensityAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val glyphIndex = parameters[WidgetKeys.GlyphIndexKey] ?: return
        val isRed = glyphIndex == 6

        var newIntensitiesStr = DEFAULT_INTENSITIES
        var newIntensity = 0

        updateAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId
        ) { prefs ->
            val intensities = (prefs[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES)
                .split(",")
                .map { it.toIntOrNull() ?: 0 }
                .toMutableList()

            newIntensity = cycleIntensity(intensities[glyphIndex], isRed)
            intensities[glyphIndex] = newIntensity
            newIntensitiesStr = intensities.joinToString(",")

            prefs.toMutablePreferences().apply {
                this[INTENSITIES_KEY] = newIntensitiesStr
            }
        }

        // ── Fire the physical glyph hardware ────────────────────────────────
        val channels = listOf(
            Glyph.Code_25111.A_1,
            Glyph.Code_25111.A_2,
            Glyph.Code_25111.A_3,
            Glyph.Code_25111.A_4,
            Glyph.Code_25111.A_5,
            Glyph.Code_25111.A_6,
            Glyph.Code_22111.E1
        )
        val finalIntensities = newIntensitiesStr.split(",").map { it.toIntOrNull() ?: 0 }
        val glyphController = GlyphController.getInstance(context)
        val intensityMap = channels
            .mapIndexed { i, ch -> ch to finalIntensities[i] }
            .toMap()
        
        // Reduced duration to 500ms for snappier widget feedback
        glyphController.applyGlyphStateWithIntensities(intensityMap, 500)

        // Redundant setRedGlyph removed as applyGlyphStateWithIntensities 
        // already includes the 7th channel (E1).

        // ── Redraw both widget types ─────────────────────────────────────────
        GlyphComposerHorizontalWidget().updateAll(context)
        GlyphComposerVerticalWidget().updateAll(context)
    }
}
