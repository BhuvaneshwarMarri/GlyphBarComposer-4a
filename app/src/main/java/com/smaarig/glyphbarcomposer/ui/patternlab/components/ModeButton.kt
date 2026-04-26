package com.smaarig.glyphbarcomposer.ui.patternlab.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(48.dp).clickable { onClick() },
        color = if (selected) Color.White else Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = if (!selected) BorderStroke(1.dp, Color(0xFF222222)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}
