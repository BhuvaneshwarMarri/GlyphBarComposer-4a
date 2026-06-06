package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height

@Composable
fun GlyphsColumn(
    selectedChannelIndex: Int,
    glyphIntensities: List<Int>,
    playbackIntensities: List<Int>,
    isPlaying: Boolean,
    onIntensityChange: (Int, Int) -> Unit,
    onChannelSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "GLYPHS",
                color = Color(0xFF666666),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.width(70.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        repeat(7) { index ->
            val isRed = index == 6
            val isSelected = selectedChannelIndex == index
            val intensity = if (isPlaying) playbackIntensities[index] else glyphIntensities[index]

            GlyphSquareButton(
                index = index,
                intensity = intensity,
                isSelected = isSelected,
                isRed = isRed,
                onIntensityChange = { newVal ->
                    onIntensityChange(index, newVal)
                    onChannelSelect(index)
                },
                onSelect = { onChannelSelect(index) },
                enabled = !isPlaying,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("glyph_button_$index")
            )
            Spacer(Modifier.height(2.dp))
        }
    }
}
