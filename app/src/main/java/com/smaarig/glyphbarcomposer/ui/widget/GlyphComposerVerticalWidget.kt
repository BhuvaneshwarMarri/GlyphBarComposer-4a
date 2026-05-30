package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

/**
 * Home-screen widget — V2 layout.
 *
 * Mirrors the GlyphsColumn from the V2 composer portrait screen:
 *   • Header label "<- GLYPH ->" centred at the top.
 *   • A vertical column of 7 [GlyphPickerRow]s (6 white + 1 red).
 *   • Each row:
 *       – A 6 dp selection dot (white when active, transparent otherwise).
 *       – A [ScrollPickerCell] (54×44 dp, rounded 8 dp) with:
 *           • Side texture dots (2 × 7 small dots, matching the Canvas in
 *             GlyphScrollPicker) rendered as tiny 2×2 dp boxes.
 *           • A full-height intensity-colour fill in the centre.
 *       – Tapping cycles intensity via [CycleIntensityAction].
 *   • A thin horizontal divider (60 dp wide) separates glyphs 1–6 from the
 *     red glyph (index 6), with 12 dp spacing above/below, identical to
 *     GlyphsColumn's HorizontalDivider.
 *
 * Note: Glance does not support infinite LazyRow, so the picker cell uses a
 * static tap-to-cycle interaction instead of drag scrolling.
 *
 * Minimum recommended widget size: 2 × 5 cells (~130 × 340 dp).
 */
class GlyphComposerVerticalWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> =
        PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    // ── Root layout ──────────────────────────────────────────────────────────

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()
        val intensities = (prefs[INTENSITIES_KEY] ?: DEFAULT_INTENSITIES)
            .split(",")
            .map { it.toIntOrNull() ?: 0 }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(vertical = 12.dp, horizontal = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Top
        ) {
            // ── Column header – matches "<- GLYPH ->" label in GlyphsColumn ─
            Text(
                text = "<- GLYPH ->",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF666666)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(16.dp))

            // ── 7 glyph rows ────────────────────────────────────────────────
            repeat(7) { index ->
                val isRed = index == 6
                val intensity = intensities.getOrElse(index) { 0 }

                if (isRed) {
                    // ── Divider before red glyph ─────────────────────────
                    Spacer(GlanceModifier.height(12.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(60.dp)
                            .height(1.dp)
                            .background(Color(0xFF333333))
                    ) {}
                    Spacer(GlanceModifier.height(12.dp))
                }

                GlyphPickerRow(
                    glyphIndex = index,
                    intensity = intensity,
                    isRed = isRed
                )

                // Inter-row spacing (skip after red – it's the last item)
                if (!isRed) Spacer(GlanceModifier.height(10.dp))
            }
        }
    }

    // ── Single row: selection dot + picker cell ──────────────────────────────

    @Composable
    private fun GlyphPickerRow(
        glyphIndex: Int,
        intensity: Int,
        isRed: Boolean
    ) {
        val isSelected = intensity > 0

        Row(
            modifier = GlanceModifier.wrapContentSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selection dot – 6×6 dp circle, white when active
            // Glance doesn't support CircleShape; use a square with cornerRadius = half size.
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .cornerRadius(3.dp)
                    .background(
                        if (isSelected) Color(0xFFFFFFFF) else Color(0x00000000)
                    )
            ) {}

            Spacer(GlanceModifier.width(8.dp))

            ScrollPickerCell(glyphIndex, intensity, isRed)
        }
    }

    // ── Picker cell body ─────────────────────────────────────────────────────
    // Matches GlyphScrollPicker outer Box: (cellWidth=54+16) × 44 dp.
    // Since Glance cannot draw Canvas, the 7 side-texture dots per side are
    // rendered as 7 stacked 2×2 Box items with 3 dp spacing → visually faithful.

    @Composable
    private fun ScrollPickerCell(
        glyphIndex: Int,
        intensity: Int,
        isRed: Boolean
    ) {
        val fillColor = getIntensityColor(intensity)

        // Outer Box matches total width: 54 + 16 = 70 dp, height 44 dp
        Box(
            modifier = GlanceModifier
                .size(width = 70.dp, height = 44.dp),
            contentAlignment = Alignment.Center
        ) {
            // ── Side texture dots (left column, right column) ────────────
            // Canvas draws 7 dots each side at spacing = 5 dp, centred vertically.
            // Approximated here as two stacked Column items overlaid at the edges.
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextureDotColumn()          // left side

                Spacer(GlanceModifier.defaultWeight())

                // ── Inner picker box (54×44 dp, rounded 8 dp) ───────────
                Box(
                    modifier = GlanceModifier
                        .size(width = 54.dp, height = 44.dp)
                        .cornerRadius(8.dp)
                        // Border: 1 dp line, simulated with outer darker bg + padding
                        .background(Color(0xFF3A3A3A))
                        .padding(1.dp)
                        .background(Color(0xFF111111))
                        .cornerRadius(7.dp)
                        .clickable(
                            actionRunCallback<CycleIntensityAction>(
                                actionParametersOf(
                                    WidgetKeys.GlyphIndexKey to glyphIndex
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Full-bleed intensity colour (mirrors the colour box in LazyRow items)
                    Box(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .background(fillColor)
                    ) {}
                }

                Spacer(GlanceModifier.defaultWeight())

                TextureDotColumn()          // right side
            }
        }
    }

    // ── 7-dot vertical texture column ────────────────────────────────────────
    // Approximates the Canvas dots drawn in GlyphScrollPicker:
    //   dot radius ≈ 0.8 dp → 2×2 dp box
    //   dot spacing = 5 dp
    //   total height ≈ 7 × (2 + 3) = 35 dp → fits inside 44 dp cell

    @Composable
    private fun TextureDotColumn() {
        Column(
            modifier = GlanceModifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(7) { i ->
                Box(
                    modifier = GlanceModifier
                        .size(2.dp)
                        .cornerRadius(1.dp)
                        .background(Color(0xFF444444))
                ) {}
                if (i < 6) Spacer(GlanceModifier.height(3.dp))
            }
        }
    }
}

// ── Widget receiver ──────────────────────────────────────────────────────────

class GlyphComposerVerticalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerVerticalWidget()
}
