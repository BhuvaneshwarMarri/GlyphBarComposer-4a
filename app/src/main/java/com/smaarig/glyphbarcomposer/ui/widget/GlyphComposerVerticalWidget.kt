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
import androidx.glance.action.ActionParameters
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

class GlyphComposerVerticalWidget : GlanceAppWidget() {

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
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = "<- GLYPH ->",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF666666)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = GlanceModifier.height(16.dp))

            Column(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(7) { index ->
                    val isRed = index == 6
                    if (isRed) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Box(
                            modifier = GlanceModifier
                                .width(60.dp)
                                .height(1.dp)
                                .background(Color(0xFF333333))
                        ) {}
                        Spacer(modifier = GlanceModifier.height(8.dp))
                    }

                    GlyphRow(index, intensities.getOrElse(index) { 0 })
                    if (index < 6) Spacer(modifier = GlanceModifier.height(10.dp))
                }
            }
        }
    }

    @Composable
    private fun GlyphRow(glyphIndex: Int, currentIntensity: Int) {
        val isSelected = currentIntensity > 0

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selection Dot
            Box(
                modifier = GlanceModifier
                    .size(6.dp)
                    .cornerRadius(3.dp)
                    .background(if (isSelected) Color.White else Color.Transparent)
            ) {}

            Spacer(modifier = GlanceModifier.width(8.dp))

            IntensityPicker(glyphIndex, currentIntensity)
        }
    }

    @Composable
    private fun IntensityPicker(glyphIndex: Int, currentIntensity: Int) {
        val color = getIntensityColor(currentIntensity)
        
        Box(
            modifier = GlanceModifier
                .width(54.dp)
                .height(38.dp)
                .cornerRadius(8.dp)
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
            // Side Dots (Texture)
            Row(
                modifier = GlanceModifier.fillMaxSize().padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Dot
                Box(
                    modifier = GlanceModifier.size(2.dp).cornerRadius(1.dp).background(Color(0xFF444444))
                ) {}
                
                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // Intensity Color Box
                Box(
                    modifier = GlanceModifier
                        .fillMaxHeight()
                        .width(40.dp) // Adjusted to leave space for side dots
                        .background(color)
                ) {}

                Spacer(modifier = GlanceModifier.defaultWeight())
                
                // Right Dot
                Box(
                    modifier = GlanceModifier.size(2.dp).cornerRadius(1.dp).background(Color(0xFF444444))
                ) {}
            }
        }
    }
}

class GlyphComposerVerticalWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlyphComposerVerticalWidget()
}
