package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun LibNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) {
        if (label == "SEQUENCES") Color(0xFF0086EA) else Color(0xFFFFC1CC)
    } else Color.Transparent

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() },
        color = bg,
        shape = RoundedCornerShape(28.dp),
        border = if (!selected) BorderStroke(1.dp, Color(0xFF222222)) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                null,
                tint = if (selected) Color.Black else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Text(
                label,
                color = if (selected) Color.Black else Color.Gray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                fontFamily = nothingFont
            )
        }
    }
}
