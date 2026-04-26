package com.smaarig.glyphbarcomposer.ui.studio.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.viewmodel.BeatAlgorithm

@Composable
fun AnalysisOverlay(algo: BeatAlgorithm, complete: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (complete) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(64.dp))
                Text("ANALYSIS COMPLETE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                Text("Your track is ready for synchronization", color = Color(0xFF666666), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                val squares = List(7) { i ->
                    rememberInfiniteTransition(label = "sq$i").animateFloat(
                        initialValue = 0.1f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 100), RepeatMode.Reverse),
                        label = "a$i"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    squares.forEachIndexed { i, alpha ->
                        Box(
                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                                .background((if (i == 6) Color(0xFFFF1744) else Color.White).copy(alpha = alpha.value))
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ANALYZING WITH ${algo.displayName.uppercase()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                    if (algo == BeatAlgorithm.MANUAL_EDIT) {
                        Text(algo.description, color = Color(0xFF444444), fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 50.dp), fontWeight = FontWeight.Bold)
                    }
                }
                
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF00C853), strokeWidth = 3.dp)
            }
        }
    }
}
