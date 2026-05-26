package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NotificationCategory(
    val id: String,
    val label: String,
    val icon: ImageVector
)

val defaultCategories = listOf(
    NotificationCategory("ALL", "All Notifications", Icons.Default.Notifications),
    NotificationCategory("MESSAGES", "Messages", Icons.Default.Message),
    NotificationCategory("CALLS", "Calls", Icons.Default.Call),
    NotificationCategory("DOWNLOADS", "Downloads", Icons.Default.Download),
    NotificationCategory("ALERTS", "Alerts", Icons.Default.Warning),
    NotificationCategory("SOCIAL", "Social", Icons.Default.Share),
    NotificationCategory("SYSTEM", "System", Icons.Default.Settings)
)

@Composable
fun NotificationTypeSelector(
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.height(200.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(defaultCategories) { category ->
            val isSelected = category.id == selectedCategoryId
            Surface(
                onClick = { onCategorySelected(category.id) },
                color = if (isSelected) Color.White else Color(0xFF2A2A2A),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        category.icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.Black else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        category.label,
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
