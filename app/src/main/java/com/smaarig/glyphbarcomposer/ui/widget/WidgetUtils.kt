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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

import kotlinx.coroutines.sync.withLock

// ─── Shared preference key ──────────────────────────────────────────────────
val INTENSITIES_KEY = stringPreferencesKey("intensities")
const val DEFAULT_INTENSITIES = "0,0,0,0,0,0,0"

// ─── Widget action parameter keys ───────────────────────────────────────────
object WidgetKeys {
    val GlyphIndexKey = ActionParameters.Key<Int>("glyph_index")
}

// ─── Intensity colour palette ───────────────────────────────────────────────
fun getIntensityColor(intensity: Int, isRed: Boolean = false): Color {
    // Standardise on 0-3 scale for hardware, map to red palette for UI if index 6
    val finalIntensity = if (isRed && intensity > 0 && intensity < 4) 6 else intensity
    return when (finalIntensity) {
        0 -> Color(0xFF1C1C1C) // OFF
        1 -> Color(0xFF686868) // LOW white
        2 -> Color(0xFFCDCDCD) // MED white
        3 -> Color(0xFFFFFFFF) // HIGH white
        4 -> Color(0xFFC62828) // LOW red
        5 -> Color(0xFFEF5350) // MED red
        6 -> Color(0xFFFF1744) // HIGH red
        else -> Color(0xFF1C1C1C)
    }
}

// ─── Cycle intensity states ──────────────────────────────────────────────────
fun cycleIntensity(current: Int, isRed: Boolean): Int {
    // Binary toggle: OFF -> HIGH (3) -> OFF
    // Note: Always use 0-3 for hardware compatibility (GlyphController scale)
    return if (current == 0) 3 else 0
}

// ─── BUG-5 FIX: Singleton application-level coroutine scope and Mutex ────────
// The old code allowed concurrent DataStore writes to race each other.
// A single SupervisorJob scope and a Mutex serialise widget updates.
private val widgetUpdateScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private val widgetMutex = kotlinx.coroutines.sync.Mutex()

// ─── Cache to avoid redundant updates ──────────────────────────────────────
private var lastIntensitiesStr: String? = null
private var lastIsPlaying: Boolean? = null
private var lastPlaylistId: Long? = null

/**
 * Unified helper to sync ALL app widgets with the current global state.
 *
 * Latency Fix: Uses state diffing to skip redundant DataStore writes and
 * UI refreshes. Critical for high-frequency playback sync.
 */
fun updateAllWidgets(
    context: Context,
    intensities: List<Int>? = null,
    isPlaying: Boolean? = null,
    playlistId: Long? = null,
    playlistName: String? = null
) {
    val intensityStr = intensities?.joinToString(",")
    val manager = GlanceAppWidgetManager(context)

    // Quick-check before launching a job
    if (intensityStr == lastIntensitiesStr && 
        isPlaying == lastIsPlaying && 
        playlistId == lastPlaylistId) {
        return
    }

    widgetUpdateScope.launch {
        widgetMutex.withLock {
            // Re-check inside lock for race conditions
            if (intensityStr == lastIntensitiesStr && 
                isPlaying == lastIsPlaying && 
                playlistId == lastPlaylistId) {
                return@withLock
            }

            var intensityUpdated = false
            var playerStateUpdated = false

            // 1. Update Intensity Widgets (Horizontal & Vertical)
            if (intensityStr != null && intensityStr != lastIntensitiesStr) {
                listOf(
                    GlyphComposerHorizontalWidget::class.java,
                    GlyphComposerVerticalWidget::class.java
                ).forEach { widgetClass ->
                    val glanceIds = manager.getGlanceIds(widgetClass)
                    glanceIds.forEach { glanceId ->
                        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                            prefs.toMutablePreferences().apply {
                                this[INTENSITIES_KEY] = intensityStr
                            }
                        }
                    }
                }
                intensityUpdated = true
                lastIntensitiesStr = intensityStr
            }

            // 2. Update Sequence Player Widget
            val hasPlayerChanges = (isPlaying != null && isPlaying != lastIsPlaying) ||
                                 (intensityStr != null && intensityStr != lastIntensitiesStr) ||
                                 (playlistId != null && playlistId != lastPlaylistId)

            if (hasPlayerChanges) {
                val playerGlanceIds = manager.getGlanceIds(GlyphSequencePlayerWidget::class.java)
                playerGlanceIds.forEach { glanceId ->
                    updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
                        prefs.toMutablePreferences().apply {
                            if (isPlaying != null) this[GlyphSequencePlayerWidget.IS_PLAYING] = isPlaying
                            if (intensityStr != null) this[INTENSITIES_KEY] = intensityStr
                            if (playlistId != null) this[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_ID] = playlistId
                            if (playlistName != null) this[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_NAME] = playlistName
                        }
                    }
                }
                playerStateUpdated = true
            }
            
            // Update cache after processing both widget types
            if (intensityStr != null) lastIntensitiesStr = intensityStr
            if (isPlaying != null) lastIsPlaying = isPlaying
            if (playlistId != null) lastPlaylistId = playlistId

            // 3. Trigger visual refresh ONLY for widgets that changed
            if (intensityUpdated) {
                GlyphComposerHorizontalWidget().updateAll(context)
                GlyphComposerVerticalWidget().updateAll(context)
            }
            if (playerStateUpdated) {
                GlyphSequencePlayerWidget().updateAll(context)
            }
        }
    }
}

/**
 * Action to toggle a single glyph's intensity from the widget.
 * Updates hardware immediately and syncs all widget instances.
 */
class IndividualCycleAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val index = parameters[WidgetKeys.GlyphIndexKey] ?: return

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            val currentStr = prefs[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES
            val intensities = currentStr.split(",").map { it.toIntOrNull() ?: 0 }.toMutableList()

            if (index in intensities.indices) {
                // 1. Calculate new state
                val isRed = (index == 6)
                intensities[index] = cycleIntensity(intensities[index], isRed)
                
                // 2. Hardware: Update immediately
                GlyphController.getInstance(context).applyGlyphState(intensities, 0)

                // 3. UI: Sync all other widget types
                updateAllWidgets(context, intensities = intensities)

                // 4. Update this widget's local state
                prefs.toMutablePreferences().apply {
                    this[INTENSITIES_KEY] = intensities.joinToString(",")
                }
            } else {
                prefs
            }
        }
    }
}