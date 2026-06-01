package com.smaarig.glyphbarcomposer.ui.studio.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.BeatAlgorithm
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState

@Composable
fun ProjectSetupView(
    uiState: MusicStudioUiState,
    onPickFile: () -> Unit,
    onAlgorithmSelect: (BeatAlgorithm) -> Unit,
    onToggleRedGlyph: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(if (isLandscape) 24.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isLandscape) Arrangement.Top else Arrangement.Center
    ) {
        if (!isLandscape) {
            StudioLogoSection()
            Spacer(Modifier.height(40.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "MUSIC STUDIO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                )
                Button(
                    onClick = onPickFile,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AudioFile, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SELECT TRACK", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(if (isLandscape) 1f else 1f),
            color = Color(0xFF111111),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(modifier = Modifier.padding(if (isLandscape) 24.dp else 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "SYNC ENGINE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Select detection logic",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { expanded = true },
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    uiState.selectedAlgorithm.displayName.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .background(Color(0xFF1A1A1A))
                                .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        ) {
                            BeatAlgorithm.entries.forEach { algo ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                algo.displayName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                algo.description,
                                                color = Color.Gray,
                                                fontSize = 10.sp,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    },
                                    onClick = {
                                        onAlgorithmSelect(algo)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (uiState.selectedAlgorithm == BeatAlgorithm.BPM_GRID) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF080808))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Target Tempo",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onBpmChange(uiState.bpmOverride - 5) }) {
                                Text("−", color = Color.White, fontSize = 20.sp)
                            }
                            Text(
                                "${uiState.bpmOverride} BPM",
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                            IconButton(onClick = { onBpmChange(uiState.bpmOverride + 5) }) {
                                Text("+", color = Color.White, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(
                                    if (uiState.includeRedGlyph) Color(0xFFFF1744).copy(0.2f) else Color(
                                        0xFF080808
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.includeRedGlyph) Color(0xFFFF1744) else Color(
                                            0xFF333333
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "RED GLYPH SYNC",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                "Center LED interaction",
                                color = Color.Gray,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Switch(
                        checked = uiState.includeRedGlyph,
                        onCheckedChange = onToggleRedGlyph,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF1744),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF080808)
                        )
                    )
                }
            }
        }

        if (!isLandscape) {
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(64.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.AudioFile, null, Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text(
                    "PICK AUDIO FILE",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(Modifier.height(120.dp))
    }
}

@Composable
fun StudioLogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.Black)
                .border(1.dp, Color(0xFF222222), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            repeat(7) { i ->
                val angle = i * (360f / 7f)
                val r = 38.dp
                Box(
                    modifier = Modifier
                        .offset(
                            x = (r.value * kotlin.math.cos(Math.toRadians(angle.toDouble()))).dp,
                            y = (r.value * kotlin.math.sin(Math.toRadians(angle.toDouble()))).dp
                        )
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (i == 6) Color(0xFFFF1744) else Color.White.copy(alpha = 0.4f))
                )
            }
            Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text(
            "MUSIC STUDIO",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = 4.sp,
            fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Precision audio synchronization",
            color = Color(0xFF555555), fontSize = 13.sp, textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}
