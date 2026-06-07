package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun EmptyStateView(icon: ImageVector, title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = Color(0xFF222222), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text(
            title.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp,
            fontFamily = nothingFont,
            letterSpacing = 2.sp
        )
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            color = Color.Gray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            fontFamily = nothingFont,
            letterSpacing = 1.sp
        )
    }
}
