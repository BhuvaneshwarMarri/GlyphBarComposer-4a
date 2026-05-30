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
 * Home-screen widget — V1 layout.
 *
 * Mirrors the "GLYPHS" panel inside [ComposerScreenOld]:
 *   • A horizontal row of 7 [GlyphButtonCell]s (6 white + 1 red).
 *   • Each cell shows:
 *       – Label above (1–6 or "R"), dimmed unless active.
 *       – A rounded rectangle button (40×52 dp) with a full-bleed
 *         intensity-colour fill and 1–3 indicator dots at the bottom,
 *         identical to [OldGlyphButton].
 *       – A thin vertical divider separates the 6th and 7th (red) cells.
 *   • Tapping cycles intensity via [CycleIntensityAction].
 *
 * Minimum recommended widget size: 4 × 2 cells (~270 × 110 dp).
 */
class GlyphComposerHorizontalWidget : GlanceAppWidget() {

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
                .background(Color(0xFF000000))        // pure black background
                .padding(horizontal = 2.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Section label (matches "GLYPHS" header in ComposerScreenOld) ─
            Text(
                text = "GLYPHS",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF888888)),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(6.dp))

            // ── Button row ──────────────────────────────────────────────────
            Row(
                modifier = GlanceModifier.fillMaxWidth().wrapContentHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.Bottom
            ) {
                // Glyphs 1–6 (white)
                repeat(6) { index ->
                    GlyphButtonCell(
                        glyphIndex = index,
                        intensity = intensities.getOrElse(index) { 0 },
                        isRed = false
                    )
                    // Flexible spacing between white glyphs
                    if (index < 5) Spacer(GlanceModifier.defaultWeight())
                }

                // Fixed spacing around the divider
                Spacer(GlanceModifier.width(4.dp))

                // ── Divider between white and red glyphs ─────────────────
                Box(
                    modifier = GlanceModifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(Color(0xFF222222))
                ) {}

                Spacer(GlanceModifier.width(4.dp))

                // Glyph 7 – Red
                GlyphButtonCell(
                    glyphIndex = 6,
                    intensity = intensities.getOrElse(6) { 0 },
                    isRed = true
                )
            }
        }
    }

    // ── Single glyph column (label + button) ─────────────────────────────────

    @Composable
    private fun GlyphButtonCell(
        glyphIndex: Int,
        intensity: Int,
        isRed: Boolean
    ) {
        val isActive = intensity > 0

        Column(
            modifier = GlanceModifier.wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index label – white when active, dark when off (matches OldGlyphButton)
            Text(
                text = if (isRed) "R" else "${glyphIndex + 1}",
                style = TextStyle(
                    color = ColorProvider(
                        if (isActive) Color(0xFFFFFFFF) else Color(0xFF444444)
                    ),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(GlanceModifier.height(4.dp))

            GlyphButtonBody(glyphIndex, intensity, isRed)
        }
    }

    // ── Button body: rounded rect with colour fill + dot indicators ──────────
    // Matches OldGlyphButton box dimensions (approx), optimized for widget fitting.

    @Composable
    private fun GlyphButtonBody(
        glyphIndex: Int,
        intensity: Int,
        isRed: Boolean
    ) {
        val bgColor = getIntensityColor(intensity)
        val isActive = intensity > 0

        Box(
            modifier = GlanceModifier
                .size(width = 36.dp, height = 48.dp)
                .cornerRadius(8.dp)
                // Outer border: white when selected, very dark otherwise
                .background(if (isActive) Color(0xFF333333) else Color(0xFF222222))
                .padding(1.dp)
                // Inner background
                .background(if (isActive) Color(0xFF1E1E1E) else Color(0xFF111111))
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
            // Full-bleed colour fill (4 dp inset, 5 dp corner)
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .cornerRadius(5.dp)
                    .background(bgColor),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Dot indicators – shown at the bottom, 4 dp from edge
                // White dots: 3 for white glyphs, 1 for red
                Row(
                    modifier = GlanceModifier.padding(bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dotCount = if (isRed) 1 else 3
                    repeat(dotCount) { dotIdx ->
                        val threshold = if (isRed) 4 else (dotIdx + 1)
                        val dotActive =
                            isActive && (if (isRed) intensity >= 4 else intensity >= threshold)

                        Spacer(
                            modifier = GlanceModifier
                                .size(width = 8.dp, height = 2.dp)
                                .cornerRadius(1.dp)
                                .background(
                                    if (dotActive) Color(0xE6FFFFFF)   // 0.9 alpha white
                                    else           Color(0x1FFFFFFF)   // 0.12 alpha white
                                )
                        )
                        if (dotIdx < dotCount - 1) Spacer(GlanceModifier.width(2.dp))
                    }
                }
            }
        }
    }
}

// ── Widget receiver ──────────────────────────────────────────────────────────

class GlyphComposerHorizontalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerHorizontalWidget()
}
