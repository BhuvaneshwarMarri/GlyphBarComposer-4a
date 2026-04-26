package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.composer.components.common.GlyphScrollPicker
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel

@Composable
fun GlyphsColumn(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
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
            // Invisible spacer to match the selection dot width (6dp) and alignment
            Spacer(Modifier.size(6.dp))

            Text(
                "<- GLYPH ->",
                color = Color(0xFF666666),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.width(70.dp), // Matches GlyphScrollPicker width
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(Modifier.height(16.dp))

        repeat(7) { index ->
            val isRed = index == 6
            val isSelected = uiState.selectedChannelIndex == index
            val intensity = uiState.glyphIntensities[index]

            if (isRed) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Spacer(Modifier.width(4.dp))
                    HorizontalDivider(
                        modifier = Modifier.width(60.dp),
                        thickness = 1.dp,
                        color = Color(0xFF333333)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color.White else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) Color.White.copy(0.2f) else Color.Transparent,
                            CircleShape
                        )
                )

                GlyphScrollPicker(
                    intensity = intensity,
                    onIntensityChange = { newVal ->
                        viewModel.onIntensityChange(index, newVal)
                        viewModel.setSelectedChannel(index)
                    },
                    isRed = isRed,
                    enabled = !uiState.isPlaying
                )
            }

            if (!isRed) Spacer(Modifier.height(10.dp))
        }
    }
}
