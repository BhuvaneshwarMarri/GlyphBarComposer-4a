package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap

@Composable
fun WaveformRuler(
    waveform: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.drawWithCache {
            // This block is only re-executed if the size or parameters change
            val width = size.width
            val height = size.height
            val samples = waveform.size
            
            onDrawBehind {
                if (samples == 0) return@onDrawBehind
                
                val pxPerSample = width / samples
                
                // Background subtle glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF00C853).copy(alpha = 0.05f), Color.Transparent, Color(0xFF00C853).copy(alpha = 0.05f))
                    ),
                    size = size
                )

                waveform.forEachIndexed { i, energy ->
                    val x = i * pxPerSample
                    val barHeight = (energy * height * 0.8f).coerceAtLeast(2f)
                    
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF00E676).copy(alpha = 0.8f),
                                Color(0xFF00C853),
                                Color(0xFF00E676).copy(alpha = 0.8f)
                            )
                        ),
                        start = Offset(x, height / 2 - barHeight / 2),
                        end = Offset(x, height / 2 + barHeight / 2),
                        strokeWidth = (pxPerSample * 0.7f).coerceAtLeast(1.5f),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    ) {
        // No-op
    }
}
