package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun EmptyHooksView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.LinkOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "NO ACTIVE HOOKS",
            style = MaterialTheme.typography.titleLarge,
            fontFamily = nothingFont,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Hook your notifications to custom Glyph sequences and make every alert unique.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}
