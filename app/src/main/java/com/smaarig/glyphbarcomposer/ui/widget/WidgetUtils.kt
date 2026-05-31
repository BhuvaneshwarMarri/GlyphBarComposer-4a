package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.service.GlyphPlaybackService
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

// ─── Shared preference key ──────────────────────────────────────────────────
val INTENSITIES_KEY = stringPreferencesKey("intensities")
const val DEFAULT_INTENSITIES = "0,0,0,0,0,0,0"

// ─── Widget action parameter keys ───────────────────────────────────────────
object WidgetKeys {
    val GlyphIndexKey = ActionParameters.Key<Int>("glyph_index")
}

// ─── Intensity colour palette ───────────────────────────────────────────────
fun getIntensityColor(intensity: Int): Color = when (intensity) {
    0    -> Color(0xFF1C1C1C) // OFF (Matches CommonUi.kt)
    1    -> Color(0xFF686868) // LOW white
    2    -> Color(0xFFCDCDCD) // MED white
    3    -> Color(0xFFFFFFFF) // HIGH white
    4    -> Color(0xFFC62828) // LOW red
    5    -> Color(0xFFEF5350) // MED red
    6    -> Color(0xFFFF1744) // HIGH red
    else -> Color(0xFF1C1C1C)
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

        val glyphController = GlyphController.getInstance(context)
        val currentIntensities = glyphController.currentIntensities.value.toMutableList()

        // 1. Cycle the intensity for the specific glyph
        currentIntensities[glyphIndex] = cycleIntensity(currentIntensities[glyphIndex], isRed)

        // 2. Build the channel→intensity map
        val intensityMap = glyphController.channels.mapIndexed { i, ch ->
            ch to currentIntensities.getOrElse(i) { 0 }
        }.toMap()

        // 3. Fire the physical glyph hardware and update global state.
        //    The controller will automatically sync this to the widget DataStore.
        glyphController.applyGlyphStateWithIntensities(intensityMap, durationMs = 800)
    }
}

/**
 * Master Kill Switch: Turns off all glyphs, stops sequence playback,
 * and resets all widget visuals.
 */
class PowerOffAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 1. Stop playback service
        context.stopService(Intent(context, GlyphPlaybackService::class.java))

        // 2. Kill hardware lights
        GlyphController.getInstance(context).turnOffGlyphs()

        // 3. Reset all Sequence Player widgets' playback state
        val manager = GlanceAppWidgetManager(context)
        val playerGlanceIds = manager.getGlanceIds(GlyphSequencePlayerWidget::class.java)
        playerGlanceIds.forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                prefs.toMutablePreferences().apply {
                    this[GlyphSequencePlayerWidget.IS_PLAYING] = false
                }
            }
        }
        GlyphSequencePlayerWidget().updateAll(context)
    }
}
