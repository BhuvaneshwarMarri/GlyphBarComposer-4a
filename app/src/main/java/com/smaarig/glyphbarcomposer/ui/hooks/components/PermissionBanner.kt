package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionBanner(onGrantClick: () -> Unit) {
    Surface(
        color = Color(0xFF332222),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Notification Access Required", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Enable access to sync glyphs with notifications.", color = Color.LightGray, fontSize = 12.sp)
            }
            TextButton(onClick = onGrantClick) {
                Text("GRANT", color = Color.White)
            }
        }
    }
}
