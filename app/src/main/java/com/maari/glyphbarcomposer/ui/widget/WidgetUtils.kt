package com.maari.glyphbarcomposer.ui.widget

import android.content.Context
import androidx.annotation.Keep
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
import com.maari.glyphbarcomposer.controller.GlyphController
import com.maari.glyphbarcomposer.service.GlyphPlaybackService
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
    0 -> Color(0xFF1C1C1C) // OFF
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
    return if (isRed) {
        if (current == 0) 6 else 0
    } else {
        if (current == 0) 3 else 0
    }
}

class IndividualCycleAction : ActionCallback {

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val glyphIndex = parameters[WidgetKeys.GlyphIndexKey] ?: return
        val isRed = glyphIndex == 6

        val state = androidx.glance.appwidget.state.getAppWidgetState(
            context,
            PreferencesGlanceStateDefinition,
            glanceId
        )
        
        val currentStr = state[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES
        val currentIntensities = currentStr.split(",").map { it.toIntOrNull() ?: 0 }.toMutableList()

        currentIntensities[glyphIndex] = cycleIntensity(currentIntensities[glyphIndex], isRed)
        val newIntensityStr = currentIntensities.joinToString(",")

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                this[INTENSITIES_KEY] = newIntensityStr
            }
        }
        GlyphComposerVerticalWidget().update(context, glanceId)
        GlyphComposerHorizontalWidget().update(context, glanceId)

        val glyphController = GlyphController.getInstance(context)
        
        if (glyphController.activeOwner.value == GlyphController.GlyphOwner.COMPOSER) {
            glyphController.releaseControl(GlyphController.GlyphOwner.COMPOSER)
        }

        glyphController.restoreStateFromWidget(currentIntensities)

        val intensityMap = glyphController.channels.mapIndexed { i, ch ->
            ch to currentIntensities.getOrElse(i) { 0 }
        }.toMap()

        glyphController.applyGlyphStateWithIntensities(
            intensityMap, 
            durationMs = 800, 
            owner = GlyphController.GlyphOwner.WIDGET
        )

        updateAllWidgets(context, intensities = currentIntensities)
    }
}

@Keep
class PowerOffAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.stopService(Intent(context, GlyphPlaybackService::class.java))

        GlyphController.getInstance(context).turnOffGlyphs()

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

suspend fun updateAllWidgets(
    context: Context,
    intensities: List<Int>? = null,
    isPlaying: Boolean? = null,
    playlistId: Long? = null,
    playlistName: String? = null
) {
    val intensityStr = intensities?.joinToString(",")
    val manager = GlanceAppWidgetManager(context)
    
    manager.getGlanceIds(GlyphComposerHorizontalWidget::class.java).forEach { glanceId ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (intensityStr != null) this[INTENSITIES_KEY] = intensityStr
            }
        }
    }
    manager.getGlanceIds(GlyphComposerVerticalWidget::class.java).forEach { glanceId ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            prefs.toMutablePreferences().apply {
                if (intensityStr != null) this[INTENSITIES_KEY] = intensityStr
            }
        }
    }
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
    GlyphComposerHorizontalWidget().updateAll(context)
    GlyphComposerVerticalWidget().updateAll(context)
    GlyphSequencePlayerWidget().updateAll(context)
}
