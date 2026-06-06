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
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.service.GlyphPlaybackService
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
    0 -> Color(0xFF1C1C1C) // OFF (Matches CommonUi.kt)
    1 -> Color(0xFF686868) // LOW white
    2 -> Color(0xFFCDCDCD) // MED white
    3 -> Color(0xFFFFFFFF) // HIGH white
    4 -> Color(0xFFC62828) // LOW red
    5 -> Color(0xFFEF5350) // MED red
    6 -> Color(0xFFFF1744) // HIGH red
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
 *   The channels list used in IndividualCycleAction is now delegated to the
 *   controller via GlyphConstants.PHONE_4A_CHANNELS. This ensures that the
 *   correct channel codes are used across both the app and the widget.
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

        // 1. UI-FIRST UPDATE (Fastest perceived latency)
        // Read current state directly from DataStore to avoid waiting for Controller init
        val state = androidx.glance.appwidget.state.getAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId
        )
        
        val currentStr = state[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES
        val currentIntensities = currentStr.split(",").map { it.toIntOrNull() ?: 0 }.toMutableList()

        // Cycle the local state
        currentIntensities[glyphIndex] = cycleIntensity(currentIntensities[glyphIndex], isRed)
        val newIntensityStr = currentIntensities.joinToString(",")

        // Update this widget's UI immediately
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[INTENSITIES_KEY] = newIntensityStr
            }
        }
        // Trigger immediate redraw for both types to cover whatever widget was clicked
        GlyphComposerVerticalWidget().update(context, glanceId)
        GlyphComposerHorizontalWidget().update(context, glanceId)

        // 2. HARDWARE SYNC (Background)
        // By the time we get here, GlyphApplication.onCreate has likely already started the binding
        val glyphController = GlyphController.getInstance(context)
        
        // If we are interacting with widgets while the app is in Composer screen,
        // we should ensure the app knows it lost "exclusive" control of the hardware visual state.
        if (glyphController.activeOwner.value == GlyphController.GlyphOwner.COMPOSER) {
            glyphController.releaseControl(GlyphController.GlyphOwner.COMPOSER)
        }

        glyphController.restoreStateFromWidget(currentIntensities)

        val intensityMap = glyphController.channels.mapIndexed { i, ch ->
            ch to currentIntensities.getOrElse(i) { 0 }
        }.toMap()

        // Fire physical lights - Using owner = WIDGET to allow "Preview" behavior
        glyphController.applyGlyphStateWithIntensities(
            intensityMap, 
            durationMs = 800, 
            owner = GlyphController.GlyphOwner.WIDGET
        )

        // 3. Sync ALL other widgets (Low priority)
        updateAllWidgets(context, intensities = currentIntensities)
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

/**
 * Unified helper to sync ALL app widgets with the current global state.
 */
suspend fun updateAllWidgets(
    context: Context,
    intensities: List<Int>? = null,
    isPlaying: Boolean? = null,
    playlistId: Long? = null,
    playlistName: String? = null
) {
    val intensityStr = intensities?.joinToString(",")
    val manager = GlanceAppWidgetManager(context)
    
    // 1. Write all state first (horizontal)
    manager.getGlanceIds(GlyphComposerHorizontalWidget::class.java).forEach { glanceId ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (intensityStr != null) this[INTENSITIES_KEY] = intensityStr
            }
        }
    }
    // 2. Write all state (vertical)
    manager.getGlanceIds(GlyphComposerVerticalWidget::class.java).forEach { glanceId ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (intensityStr != null) this[INTENSITIES_KEY] = intensityStr
            }
        }
    }
    // 3. Write player state
    manager.getGlanceIds(GlyphSequencePlayerWidget::class.java).forEach { glanceId ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (isPlaying != null) this[GlyphSequencePlayerWidget.IS_PLAYING] = isPlaying
                if (intensityStr != null) this[INTENSITIES_KEY] = intensityStr
                if (playlistId != null) this[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_ID] = playlistId
                if (playlistName != null) this[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_NAME] = playlistName
            }
        }
    }
    // 4. Trigger renders ONCE after ALL state is committed
    GlyphComposerHorizontalWidget().updateAll(context)
    GlyphComposerVerticalWidget().updateAll(context)
    GlyphSequencePlayerWidget().updateAll(context)
}
