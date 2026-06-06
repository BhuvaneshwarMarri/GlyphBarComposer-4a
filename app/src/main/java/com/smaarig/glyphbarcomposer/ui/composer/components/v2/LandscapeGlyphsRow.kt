package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun LandscapeGlyphsRow(
    selectedChannelIndex: Int,
    glyphIntensities: List<Int>,
    playbackIntensities: List<Int>,
    isPlaying: Boolean,
    onIntensityChange: (Int, Int) -> Unit,
    onChannelSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(20.dp))
            .padding(12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val isRed = index == 6
            val isSelected = selectedChannelIndex == index
            val intensity = if (isPlaying) playbackIntensities[index] else glyphIntensities[index]

            if (isRed) {
                Box(modifier = Modifier
                    .width(1.dp)
                    .height(62.dp)
                    .background(Color(0xFF2A2A2A)))
            }

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
                modifier = Modifier.testTag("glyph_button_landscape_$index")
            )
        }
    }
}
