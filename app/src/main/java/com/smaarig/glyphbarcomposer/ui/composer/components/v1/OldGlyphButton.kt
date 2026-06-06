package com.smaarig.glyphbarcomposer.ui.composer.components.v1

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.composer.components.v2.GlyphSquareButton

@Composable
fun OldGlyphButton(
    index: Int,
    intensity: Int,
    isSelected: Boolean,
    isRed: Boolean,
    onIntensityChange: (Int) -> Unit,
    onSelect: () -> Unit,
    enabled: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (isRed) "RED" else "${index + 1}",
            color = if (isSelected) Color.White else Color(0xFF444444),
            fontSize = 7.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )

        GlyphSquareButton(
            index = index,
            intensity = intensity,
            isSelected = isSelected,
            isRed = isRed,
            onIntensityChange = onIntensityChange,
            onSelect = onSelect,
            enabled = enabled,
            modifier = Modifier.size(52.dp)
        )
    }
}
