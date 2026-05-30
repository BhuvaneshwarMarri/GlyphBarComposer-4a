package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition

/**
 * Minimalist Glyph Composer Widget.
 * Clean, square buttons for binary control of glyphs.
 * Labels and secondary backgrounds removed for a modern look.
 */
class GlyphComposerHorizontalWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val intensities = (prefs[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES)
            .split(",")
            .map { it.toIntOrNull() ?: 0 }

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── White glyphs 1-6 ──────────────────────────────────────
                repeat(6) { index ->
                    GlyphSquareButton(
                        index = index,
                        intensity = intensities.getOrElse(index) { 0 }
                    )
                    if (index < 5) Spacer(GlanceModifier.width(4.dp))
                }

                // ── Subtle divider ─────────────────────────────────────────
                Box(
                    modifier = GlanceModifier
                        .padding(horizontal = 6.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0xFF333333))
                ) {}

                // ── Red glyph (index 6) ────────────────────────────────────
                GlyphSquareButton(
                    index = 6,
                    intensity = intensities.getOrElse(6) { 0 }
                )
            }
        }
    }

    @Composable
    private fun GlyphSquareButton(index: Int, intensity: Int) {
        val statusColor = getIntensityColor(intensity)

        Box(
            modifier = GlanceModifier
                .size(32.dp)
                .cornerRadius(8.dp)
                .background(statusColor)
                .clickable(
                    actionRunCallback<IndividualCycleAction>(
                        actionParametersOf(WidgetKeys.GlyphIndexKey to index)
                    )
                )
        ) {}
    }
}

class GlyphComposerHorizontalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerHorizontalWidget()
}
