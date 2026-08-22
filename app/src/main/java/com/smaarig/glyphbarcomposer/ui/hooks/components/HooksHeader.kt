package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.ui.components.ScreenHeader

@Composable
fun HooksHeader(onAddClick: () -> Unit) {
    ScreenHeader(
        title = "HOOKS",
        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        actions = {
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
    )
}
