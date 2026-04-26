package com.smaarig.glyphbarcomposer.ui.composer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel

@Composable
fun ComposerHeader(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    powerScale: Float,
    onPowerClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "COMPOSER",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF111111))
                    .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                    .padding(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("V2" to false, "V1" to true).forEach { (label, isV1) ->
                        val selected = uiState.useOldVersion == isV1
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (selected) {
                                        if (isV1) Color(0xFFFFEB3B) else Color(0xFFFFC1CC)
                                    } else Color.Transparent
                                )
                                .clickable { viewModel.toggleVersion(isV1) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (selected) Color.Black else Color(0xFF555555),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                            )
                        }
                    }
                }
            }

            if (uiState.currentSequenceSteps.isNotEmpty()) {
                IconButton(onClick = viewModel::clearSequence, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.DeleteSweep, "Clear",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            IconButton(
                onClick = onPowerClick,
                modifier = Modifier
                    .size(32.dp)
                    .scale(powerScale)
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew, "Turn Off All",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
