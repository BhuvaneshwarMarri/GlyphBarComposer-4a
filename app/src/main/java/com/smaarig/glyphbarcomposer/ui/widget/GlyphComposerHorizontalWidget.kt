package com.smaarig.glyphbarcomposer.ui.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.controller.GlyphController

class GlyphComposerHorizontalWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = androidx.glance.currentState<Preferences>()
        val intensitiesStr = prefs[stringPreferencesKey("intensities")] ?: "0,0,0,0,0,0,0"
        val intensities = intensitiesStr.split(",").map { it.toIntOrNull() ?: 0 }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GLYPHS",
                style = TextStyle(
                    color = ColorProvider(Color.Gray),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(7) { index ->
                    GlyphColumn(index, intensities.getOrElse(index) { 0 })
                    if (index < 6) Spacer(modifier = GlanceModifier.width(6.dp))
                }
            }
        }
    }

    @Composable
    private fun GlyphColumn(glyphIndex: Int, currentIntensity: Int) {
        val isRed = glyphIndex == 6

        Column(
            modifier = GlanceModifier.wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRed) "R" else "${glyphIndex + 1}",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF444444)),
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))
            
            IntensitySquare(glyphIndex, currentIntensity)
        }
    }

    @Composable
    private fun IntensitySquare(glyphIndex: Int, currentIntensity: Int) {
        val color = getIntensityColor(currentIntensity)
        val isRed = glyphIndex == 6
        
        Box(
            modifier = GlanceModifier
                .size(width = 34.dp, height = 48.dp)
                .cornerRadius(10.dp)
                .background(Color(0xFF111111))
                // Simulated border using padding and nested box if needed, 
                // but cornerRadius with background is usually enough for v1 look.
                // We'll add a 1dp padding and a darker background to simulate border if possible.
                .padding(1.dp)
                .background(Color(0xFF222222))
                .padding(0.5.dp) // inner border
                .background(Color(0xFF111111))
                .clickable(
                    actionRunCallback<CycleIntensityAction>(
                        actionParametersOf(
                            WidgetKeys.GlyphIndexKey to glyphIndex
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .cornerRadius(6.dp)
                    .background(color),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Intensity Dots at the bottom
                Row(
                    modifier = GlanceModifier.padding(bottom = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val maxDots = if (isRed) 1 else 3
                    repeat(maxDots) { dotIdx ->
                        val dotLevel = if (isRed) 4 else (dotIdx + 1)
                        // For white: 1=Low, 2=Med, 3=High
                        // For red: 4=Low, 5=Med, 6=Full. 
                        // The logic in OldGlyphButton is: val active = intensity >= dotLevel && intensity > 0
                        // But for red it only has one dot representing "on".
                        val active = if (isRed) currentIntensity >= 4 else currentIntensity >= dotLevel
                        
                        Spacer(
                            modifier = GlanceModifier
                                .size(width = 10.dp, height = 2.dp)
                                .cornerRadius(1.dp)
                                .background(
                                    if (active) Color.White.copy(alpha = 0.9f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                        )
                        if (dotIdx < maxDots - 1) Spacer(GlanceModifier.width(2.dp))
                    }
                }
            }
        }
    }
}

class GlyphComposerHorizontalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerHorizontalWidget()
}
