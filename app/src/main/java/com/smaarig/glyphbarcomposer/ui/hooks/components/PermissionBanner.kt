package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
fun PermissionBanner(onGrantClick: () -> Unit) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF332222)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFFF5252),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Notification Access Required",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
                Text(
                    "Enable access to sync glyphs with notifications.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Button(
                onClick = onGrantClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    "GRANT",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}
