package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.BeatAlgorithm

@Composable
fun AnalyzerCard(
    selectedAlgorithm: BeatAlgorithm,
    includeRedGlyph: Boolean,
    isAnalyzing: Boolean,
    bpmOverride: Int,
    onAlgorithmSelect: (BeatAlgorithm) -> Unit,
    onToggleRedGlyph: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit,
    onReanalyze: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "SYNC ENGINE",
            color = Color(0xFF555555),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Algorithm Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { expanded = true },
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    selectedAlgorithm.displayName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                        Text(
                                            algo.displayName,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    },
                                    onClick = {
                                        onAlgorithmSelect(algo)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Red Toggle (Mini)
                    Box(
                        modifier = Modifier
                            .size(height = 42.dp, width = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (includeRedGlyph) Color(0xFFFF1744).copy(0.15f) else Color(
                                    0xFF1A1A1A
                                )
                            )
                            .border(
                                1.dp,
                                if (includeRedGlyph) Color(0xFFFF1744).copy(0.5f) else Color(
                                    0xFF2A2A2A
                                ),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onToggleRedGlyph(!includeRedGlyph) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Adjust,
                            null,
                            tint = if (includeRedGlyph) Color(0xFFFF1744) else Color(
                                0xFF444444
                            ),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Generate Button
                    Button(
                        onClick = onReanalyze,
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("GENERATE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (selectedAlgorithm == BeatAlgorithm.BPM_GRID) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF080808))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tempo",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { onBpmChange(bpmOverride - 5) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    "−",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Text(
                                "$bpmOverride BPM",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(
                                onClick = { onBpmChange(bpmOverride + 5) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    "+",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
