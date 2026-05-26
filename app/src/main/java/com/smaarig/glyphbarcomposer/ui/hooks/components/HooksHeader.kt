package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont

@Composable
fun HooksHeader(onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Link,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "HOOKS",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = nothingFont
            )
        }

        IconButton(
            onClick = onAddClick,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .size(36.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Hook",
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
