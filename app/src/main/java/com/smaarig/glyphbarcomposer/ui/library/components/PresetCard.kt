package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun PresetCard(preset: PresetSequence, isActive: Boolean, onClick: () -> Unit) {
    val accentColor = if (isActive) Color(0xFF0086EA) else Color(0xFF222222)
    val cardBg by animateColorAsState(if (isActive) Color(0xFF1A1A1A) else Color(0xFF111111), label = "cardBg")

    Surface(
        modifier = Modifier
            .size(width = 140.dp, height = 140.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = cardBg,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) accentColor.copy(alpha = 0.5f) else Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(
                preset.icon,
                null,
                tint = if (isActive) accentColor else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    preset.name,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    fontFamily = nothingFont
                )
                Text(
                    preset.description.uppercase(),
                    color = Color.Gray,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    fontFamily = nothingFont,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}
