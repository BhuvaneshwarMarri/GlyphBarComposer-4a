package com.smaarig.glyphbarcomposer.ui.composer.components.v2

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel

@Composable
fun ControlsColumn(
    uiState: ComposerUiState,
    viewModel: ComposerViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight(0.8f)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF141414), Color(0xFF0A0A0A))
                )
            )
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(24.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "CONTROLS",
            color = Color(0xFF666666),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                "${uiState.durationMs.toInt()}ms",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = uiState.durationMs,
                    onValueChange = viewModel::onDurationChange,
                    valueRange = 100f..2000f,
                    steps = 18,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF222222)
                    ),
                    modifier = Modifier
                        .graphicsLayer { rotationZ = -90f }
                        .requiredWidth(340.dp)
                        .testTag("duration_slider")
                )
            }
        }

        Button(
            onClick = viewModel::addStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                .testTag("add_step_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(0.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("ADD STEP", fontWeight = FontWeight.Black, fontSize = 11.sp)
        }
    }
}
