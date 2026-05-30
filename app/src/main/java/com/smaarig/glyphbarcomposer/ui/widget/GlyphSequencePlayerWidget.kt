package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.*
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.smaarig.glyphbarcomposer.service.GlyphPlaybackService
import androidx.glance.appwidget.updateAll
import androidx.glance.unit.ColorProvider
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.smaarig.glyphbarcomposer.ui.widget.getIntensityColor
import com.smaarig.glyphbarcomposer.ui.widget.INTENSITIES_KEY
import com.smaarig.glyphbarcomposer.ui.widget.DEFAULT_INTENSITIES

class GlyphSequencePlayerWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val sequenceName = prefs[SELECTED_SEQUENCE_NAME] ?: "No Sequence"
        val isPlaying = prefs[IS_PLAYING] ?: false

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Sequence Info (Left)
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = sequenceName.uppercase(),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = "GLYPH SEQUENCE",
                        style = TextStyle(
                            color = ColorProvider(Color.Gray),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }

                // 2. Controls (Right)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Power Off Button (Icon only)
                    Box(
                        modifier = GlanceModifier
                            .size(36.dp)
                            .clickable(actionRunCallback<PowerOffAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "⏻",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(8.dp))

                    // Play/Pause Button
                    Box(
                        modifier = GlanceModifier
                            .size(44.dp)
                            .cornerRadius(22.dp)
                            .background(if (isPlaying) Color(0xFFFF1744) else Color(0xFF1A1A1A))
                            .clickable(actionRunCallback<TogglePlaybackAction>()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isPlaying) "■" else "▶",
                            style = TextStyle(
                                color = ColorProvider(Color.White),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }

    companion object {
        val SELECTED_SEQUENCE_ID = longPreferencesKey("selected_sequence_id")
        val SELECTED_SEQUENCE_NAME = stringPreferencesKey("selected_sequence_name")
        val IS_PLAYING = booleanPreferencesKey("is_playing")
    }
}

class TogglePlaybackAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        var sequenceId: Long = -1L
        var isPlaying: Boolean = false

        updateAppWidgetState(context, PreferencesGlanceStateDefinition, glanceId) { prefs ->
            sequenceId = prefs[GlyphSequencePlayerWidget.SELECTED_SEQUENCE_ID] ?: -1L
            isPlaying = prefs[GlyphSequencePlayerWidget.IS_PLAYING] ?: false
            
            // Toggle state immediately for responsive UI
            if (sequenceId != -1L) {
                prefs.toMutablePreferences().apply {
                    this[GlyphSequencePlayerWidget.IS_PLAYING] = !isPlaying
                }
            } else {
                prefs
            }
        }
        
        if (sequenceId == -1L) return

        val intent = Intent(context, GlyphPlaybackService::class.java).apply {
            action = if (isPlaying) GlyphPlaybackService.ACTION_STOP else GlyphPlaybackService.ACTION_START
            putExtra(GlyphPlaybackService.EXTRA_PLAYLIST_ID, sequenceId)
        }

        if (isPlaying) {
            context.stopService(intent)
        } else {
            context.startForegroundService(intent)
        }

        // Explicitly update this specific widget instance to reflect the toggled state
        GlyphSequencePlayerWidget().update(context, glanceId)
    }
}

class GlyphSequencePlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphSequencePlayerWidget()
}
