package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.compose.ui.Alignment as ComposeAlignment
import androidx.glance.layout.Alignment as GlanceAlignment

/**
 * Glyph Individual Control Widget.
 * 7 tap targets — no background, no labels, just on/off glyph indicators.
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

        Row(
            modifier = GlanceModifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalAlignment = GlanceAlignment.CenterHorizontally,
            verticalAlignment = GlanceAlignment.CenterVertically
        ) {
            // White glyphs 1-6
            repeat(6) { index ->
                Box(
                    modifier = GlanceModifier.defaultWeight(),
                    contentAlignment = GlanceAlignment.Center
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
                contentAlignment = GlanceAlignment.Center
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

        // Circular Square parameters - Optimized size for fixed widget
        val indicatorSize = if (isActive) 35.dp else 32.dp
        val cornerRadius = 8.dp
        val backgroundColor = if (isActive) {
            getIntensityColor(intensity)
        } else {
            // Restore dark color for visibility when OFF
            Color(0xFF1A1A1A)
        }

        // Outer container slot
        Box(
            modifier = GlanceModifier
                .fillMaxHeight()
                .width(36.dp),
            contentAlignment = GlanceAlignment.Center
        ) {
            // Border container
            Box(
                modifier = GlanceModifier
                    .size(indicatorSize)
                    .cornerRadius(cornerRadius)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = GlanceAlignment.Center
            ) {
                // Main Indicator
                Box(
                    modifier = GlanceModifier
                        .size(indicatorSize - 2.dp)
                        .cornerRadius(cornerRadius - 1.dp)
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
}

/**
 * Mock UI for Compose Previews (Glance components cannot be previewed directly).
 */
@Composable
private fun MockGlyphDot(intensity: Int) {
    val isActive = intensity > 0
    val indicatorSize = if (isActive) 35.dp else 32.dp
    val color = getIntensityColor(intensity)

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(36.dp),
        contentAlignment = ComposeAlignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(indicatorSize)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = ComposeAlignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(indicatorSize - 2.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(color)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, widthDp = 260, heightDp = 56)
@Composable
fun GlyphComposerHorizontalWidgetPreview() {
    val intensities = listOf(3, 0, 0, 0, 0, 0, 6)
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = ComposeAlignment.CenterVertically
    ) {
        intensities.forEach { intensity ->
            MockGlyphDot(intensity = intensity)
        }
    }
}

class GlyphComposerHorizontalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerHorizontalWidget()
}