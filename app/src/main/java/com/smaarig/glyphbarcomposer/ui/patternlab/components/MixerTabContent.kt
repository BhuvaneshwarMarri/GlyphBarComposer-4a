package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.LabBlendMode
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel

@Composable
fun MixerTabContent(uiState: PatternLabUiState, viewModel: PatternLabViewModel, onSaveClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MERGE MODE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton("Sequential", !uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(false) }
                ModeButton("Layered", uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(true) }
            }
        }

        if (uiState.isLayered) {
            Surface(
                color = Color(0xFF111111),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF222222))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("BLEND SETTINGS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabBlendMode.entries.forEach { mode ->
                            ModeButton(
                                label = mode.name,
                                selected = uiState.blendMode == mode,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.onBlendModeChange(mode) }
                        }
                    }

                    if (uiState.blendMode == LabBlendMode.CROSSFADE) {
                        LabSliderRow("Ratio", "${(uiState.crossfadeRatio * 100).toInt()}%", uiState.crossfadeRatio, 0f..1f, onValueChange = viewModel::onCrossfadeRatioChange)
                    }

                    LabSliderRow("Delay B", "${uiState.offsetB}ms", uiState.offsetB.toFloat(), 0f..1000f, steps = 20, onValueChange = { viewModel.onOffsetBChange(it.toInt()) })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (uiState.previewSteps.isNotEmpty()) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("SAVE MIX", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}
