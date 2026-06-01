package com.smaarig.glyphbarcomposer.ui.hooks.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont
import com.smaarig.glyphbarcomposer.ui.viewmodel.AppInfo

@Composable
fun AddHookContent(
    apps: List<AppInfo>,
    playlists: List<PlaylistWithSteps>,
    onAdd: (AppInfo, String, PlaylistWithSteps, Boolean) -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1: App, 2: Config

    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var selectedPlaylist by remember { mutableStateOf<PlaylistWithSteps?>(null) }
    var isProgressSync by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isEmpty()) apps
        else apps.filter {
            it.appName.contains(
                searchQuery,
                ignoreCase = true
            ) || it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (step == 1) "SELECT APP" else "CONFIGURE HOOK",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = nothingFont,
                fontWeight = FontWeight.Bold
            )
            if (step == 2) {
                TextButton(onClick = { step = 1 }) {
                    Text("BACK", color = Color.Gray, fontFamily = nothingFont)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState > initialState) {
                    slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                } else {
                    slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                }
            },
            label = "StepTransition"
        ) { currentStep ->
            if (currentStep == 1) {
                Column {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search apps...", color = Color.Gray) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF2A2A2A),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppSelector(filteredApps, selectedApp) {
                        selectedApp = it
                        step = 2
                    }
                }
            } else {
                Column {
                    selectedApp?.let {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            it.icon?.let { icon ->
                                Image(
                                    bitmap = icon.toBitmap().asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(it.appName, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "NOTIFICATION TYPE",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = nothingFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    NotificationTypeSelector(selectedCategoryId) { selectedCategoryId = it }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "GLYPH SEQUENCE",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontFamily = nothingFont
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PlaylistSelector(playlists, selectedPlaylist) { selectedPlaylist = it }

                    Spacer(modifier = Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isProgressSync,
                            onCheckedChange = { isProgressSync = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color.White,
                                checkmarkColor = Color.Black
                            )
                        )
                        Text("Sync with progress bar", color = Color.White, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (selectedApp != null && selectedPlaylist != null) {
                                onAdd(
                                    selectedApp!!,
                                    selectedCategoryId,
                                    selectedPlaylist!!,
                                    isProgressSync
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedApp != null && selectedPlaylist != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "CREATE HOOK",
                            color = Color.Black,
                            fontFamily = nothingFont,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppSelector(apps: List<AppInfo>, selected: AppInfo?, onSelect: (AppInfo) -> Unit) {
    LazyColumn(modifier = Modifier.height(400.dp)) {
        items(apps) { app ->
            Surface(
                onClick = { onSelect(app) },
                color = if (selected?.packageName == app.packageName) Color(0xFF333333) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    app.icon?.let {
                        Image(
                            bitmap = it.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(app.appName, color = Color.White, fontWeight = FontWeight.Medium)
                }
            }
        }
        if (apps.isEmpty()) {
            item {
                Text("No apps found", color = Color.Gray, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
fun PlaylistSelector(
    playlists: List<PlaylistWithSteps>,
    selected: PlaylistWithSteps?,
    onSelect: (PlaylistWithSteps) -> Unit
) {
    LazyColumn(modifier = Modifier.height(150.dp)) {
        items(playlists) { playlist ->
            Surface(
                onClick = { onSelect(playlist) },
                color = if (selected?.playlist?.id == playlist.playlist.id) Color(0xFF333333) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(playlist.playlist.name, color = Color.White)
                }
            }
        }
        if (playlists.isEmpty()) {
            item {
                Text(
                    "No sequences found in library",
                    color = Color.Gray,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
