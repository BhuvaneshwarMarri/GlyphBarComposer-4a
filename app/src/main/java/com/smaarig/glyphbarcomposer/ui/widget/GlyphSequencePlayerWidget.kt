package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.smaarig.glyphbarcomposer.R
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.service.GlyphPlaybackService

class GlyphSequencePlayerWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val sequenceId = prefs[SELECTED_SEQUENCE_ID]
        val sequenceName = prefs[SELECTED_SEQUENCE_NAME] ?: "No Sequence"
        val isPlaying = prefs[IS_PLAYING] ?: false

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .cornerRadius(16.dp)
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            if (sequenceId == null) {
                Column(
                    modifier = GlanceModifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = LocalContext.current.getString(R.string.widget_no_sequence_selected),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxSize().padding(horizontal = 12.dp),
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
                                color = ColorProvider(Color(0xFFB3B3B3)),
                                fontSize = 10.sp,
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
                            Image(
                                provider = ImageProvider(R.drawable.ic_power_off),
                                contentDescription = "Turn Off",
                                modifier = GlanceModifier.size(22.dp),
                                colorFilter = androidx.glance.ColorFilter.tint(
                                    ColorProvider(
                                        Color(
                                            0xFF00E676
                                        )
                                    )
                                )
                            )
                        }

                        Spacer(modifier = GlanceModifier.width(12.dp))

                        // Play/Pause Button
                        Box(
                            modifier = GlanceModifier
                                .size(48.dp)
                                .cornerRadius(24.dp)
                                .background(Color(0xFF1DB954))
                                .clickable(actionRunCallback<TogglePlaybackAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPlaying) "■" else "▶",
                                style = TextStyle(
                                    color = ColorProvider(Color.Black),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
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
            action =
                if (isPlaying) GlyphPlaybackService.ACTION_STOP else GlyphPlaybackService.ACTION_START
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

class PowerOffAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        // 1. Stop Service
        val intent = Intent(context, GlyphPlaybackService::class.java).apply {
            action = GlyphPlaybackService.ACTION_STOP
        }
        context.stopService(intent)

        // 2. Hardware: Turn Off
        GlyphController.getInstance(context).turnOffGlyphs()

        // 3. UI: Sync all widgets to OFF state
        val offIntensities = listOf(0, 0, 0, 0, 0, 0, 0)
        updateAllWidgets(
            context = context,
            intensities = offIntensities,
            isPlaying = false
        )
    }
}

class GlyphSequencePlayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphSequencePlayerWidget()
}
