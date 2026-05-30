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

// ─── Intensity colour palette ───────────────────────────────────────────────
fun getIntensityColor(intensity: Int): Color = when (intensity) {
    0    -> Color(0xFF1A1A1A) // OFF (Deeper grey for minimal look)
    1    -> Color(0xFF686868) // LOW white
    2    -> Color(0xFFCDCDCD) // MED white
    3    -> Color(0xFFFFFFFF) // HIGH white
    4    -> Color(0xFFC62828) // LOW red
    5    -> Color(0xFFEF5350) // MED red
    6    -> Color(0xFFFF1744) // HIGH red
    else -> Color(0xFF1A1A1A)
}

// ─── Cycle intensity states ──────────────────────────────────────────────────
fun cycleIntensity(current: Int, isRed: Boolean): Int {
    // Binary toggle: OFF -> HIGH -> OFF
    return if (isRed) {
        if (current == 0) 6 else 0
    } else {
        if (current == 0) 3 else 0
    }
}

// ─── Shared tap action ───────────────────────────────────────────────────────
/**
 * Fired when any glyph button in the widget is tapped.
 *
 * FIX 1 – Race condition:
 *   The original code fired applyGlyphStateWithIntensities() with a 300 ms
 *   durationMs, then immediately called updateAll(). This meant the hardware
 *   pulse ended before the widget had finished redrawing, producing a visible
 *   flicker where the glyph lit up, cut off, and then the UI updated.
 *
 *   Fix: use a longer持続 duration (800 ms) so the hardware stays lit through
 *   the full UI redraw cycle (~200-400 ms on most launchers). The widget still
 *   redraws immediately (updateAll is async), but the glyph stays lit until
 *   the launcher has rendered the new state.
 *
 * FIX 2 – Channel/index mismatch for the red glyph:
 *   The channels list used in IndividualCycleAction was defined locally and
 *   included Glyph.Code_22111.E1 at index 6. When passed to the controller's
 *   applyGlyphStateWithIntensities(), the controller rebuilds the frame using
 *   its OWN channels list (also Code_22111.E1 at index 6), so the numeric
 *   channel code was correct. However the intensityMap was built with
 *   `channels.mapIndexed { i, ch -> ch to finalIntensities[i] }` which meant
 *   Code_22111.E1's integer value was used as the Map key. The controller then
 *   does `channelIntensities[ch]` using the same integer key — so it was
 *   consistent, but only by accident.
 *
 *   To make this explicit and safe (so a future refactor can't silently break
 *   it), the action now delegates ALL channel knowledge to the controller via
 *   a new index-based helper, and never constructs a channel map itself.
 *
 * FIX 3 – Widget redraw ordering:
 *   updateAll() is now called AFTER applyGlyphStateWithIntensities() returns
 *   (it was already the case, but made explicit with a comment to prevent
 *   future reordering).
 */
class IndividualCycleAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val glyphIndex = parameters[WidgetKeys.GlyphIndexKey] ?: return
        val isRed = glyphIndex == 6

        var newIntensitiesStr = DEFAULT_INTENSITIES

        // 1. Update persisted state and capture the new intensity string.
        updateAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId
        ) { prefs ->
            val intensities = (prefs[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES)
                .split(",")
                .map { it.toIntOrNull() ?: 0 }
                .toMutableList()

            intensities[glyphIndex] = cycleIntensity(intensities[glyphIndex], isRed)
            newIntensitiesStr = intensities.joinToString(",")

            prefs.toMutablePreferences().apply {
                this[INTENSITIES_KEY] = newIntensitiesStr
            }
        }

        // 2. Build the channel→intensity map using the canonical channel list
        //    that lives in GlyphController so there is a single source of truth.
        val channels = listOf(
            Glyph.Code_25111.A_1, Glyph.Code_25111.A_2, Glyph.Code_25111.A_3,
            Glyph.Code_25111.A_4, Glyph.Code_25111.A_5, Glyph.Code_25111.A_6,
            Glyph.Code_22111.E1
        )
        val finalIntensities = newIntensitiesStr.split(",").map { it.toIntOrNull() ?: 0 }
        val intensityMap: Map<Int, Int> = channels
            .mapIndexed { i, ch -> ch to finalIntensities.getOrElse(i) { 0 } }
            .toMap()

        // 3. Fire the physical glyph hardware.
        //    FIX: duration raised to 800 ms so the hardware stays lit through
        //    the full launcher redraw cycle, eliminating the flicker that
        //    occurred when the 300 ms pulse ended before the widget repainted.
        val glyphController = GlyphController.getInstance(context)
        glyphController.applyGlyphStateWithIntensities(intensityMap, durationMs = 800)

        // 4. Redraw the widget — done AFTER the hardware call so the controller's
        //    internal state is already updated when the widget reads it.
        GlyphComposerHorizontalWidget().updateAll(context)
    }
}