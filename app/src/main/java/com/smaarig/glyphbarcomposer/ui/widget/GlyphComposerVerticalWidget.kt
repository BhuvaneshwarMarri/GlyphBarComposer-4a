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
 * Glyph Individual Control Widget (Vertical).
 * 7 tap targets in a vertical column.
 */
class GlyphComposerVerticalWidget : GlanceAppWidget() {

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

        Column(
            modifier = GlanceModifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // White glyphs 1-6
            repeat(6) { index ->
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = Alignment.Center
                ) {
                    GlyphDot(
                        index = index,
                        intensity = intensities.getOrElse(index) { 0 },
                        isRed = false
                    )
                }
            }

            // Red glyph (index 6)
            Box(
                modifier = GlanceModifier.defaultWeight(),
                contentAlignment = Alignment.Center
            ) {
                GlyphDot(
                    index = 6,
                    intensity = intensities.getOrElse(6) { 0 },
                    isRed = true
                )
            }
        }
    }

    @Composable
    private fun GlyphDot(index: Int, intensity: Int, isRed: Boolean) {
        val isActive = intensity > 0

        // Circular Square parameters - Maximized size
        val indicatorSize = if (isActive) 38.dp else 34.dp
        val cornerRadius = 10.dp
        val backgroundColor = if (isActive) {
            getIntensityColor(intensity)
        } else {
            // Dark color for visibility when OFF
            Color(0xFF1A1A1A)
        }

        // Outer container slot
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(42.dp),
            contentAlignment = Alignment.Center
        ) {
            // Indicator "circular square" with clickable area constrained to its shape
            Box(
                modifier = GlanceModifier
                    .size(indicatorSize)
                    .cornerRadius(cornerRadius)
                    .background(backgroundColor)
                    .clickable(
                        actionRunCallback<IndividualCycleAction>(
                            actionParametersOf(WidgetKeys.GlyphIndexKey to index)
                        )
                    )
            ) {}
        }
    }
}

class GlyphComposerVerticalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerVerticalWidget()
}
