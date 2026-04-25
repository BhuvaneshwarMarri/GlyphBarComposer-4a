package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PresetCard(preset: PresetSequence, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(width = 140.dp, height = 140.dp).clickable { onClick() },
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00C853) else Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(preset.icon, null, tint = if (isActive) Color(0xFF00C853) else Color.Gray, modifier = Modifier.size(28.dp))
            Column {
                Text(preset.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(preset.description, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            }
        }
    }
}
