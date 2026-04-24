package com.smaarig.glyphbarcomposer.ui

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.automirrored.filled.*
import com.smaarig.glyphbarcomposer.ui.viewmodel.LabBlendMode
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabUiState

@Composable
fun PatternLabScreen(viewModel: PatternLabViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    var showDialogA by remember { mutableStateOf(false) }
    var showDialogB by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        PatternLabLandscape(
            uiState = uiState,
            viewModel = viewModel,
            onPickA = { showDialogA = true },
            onPickB = { showDialogB = true },
            onSaveClick = { showSaveDialog = true }
        )
    } else {
        PatternLabPortrait(
            uiState = uiState,
            viewModel = viewModel,
            onPickA = { showDialogA = true },
            onPickB = { showDialogB = true },
            onSaveClick = { showSaveDialog = true }
        )
    }

    if (showSaveDialog) {
        SaveMixDialog(
            onSave = { name: String ->
                viewModel.saveResult(name)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false }
        )
    }

    if (showDialogA) {
        PlaylistPickerDialog(
            playlists = allPlaylists,
            onSelect = { 
                viewModel.selectPlaylistA(it)
                showDialogA = false 
            },
            onDismiss = { showDialogA = false }
        )
    }
    if (showDialogB) {
        PlaylistPickerDialog(
            playlists = allPlaylists,
            onSelect = { 
                viewModel.selectPlaylistB(it)
                showDialogB = false 
            },
            onDismiss = { showDialogB = false }
        )
    }
}

@Composable
fun PatternLabPortrait(
    uiState: PatternLabUiState,
    viewModel: PatternLabViewModel,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Header
        PatternLabHeader(uiState, viewModel)

        // Tab Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF111111))
                .padding(4.dp)
        ) {
            listOf("BASE A", "BASE B", "MIXER").forEachIndexed { index, label ->
                val selected = uiState.selectedTab == index
                val bg by animateColorAsState(
                    if (selected) {
                        when (index) {
                            0 -> Color(0xFFB3E5FC) // Light Blue
                            1 -> Color(0xFFFFEB3B) // Yellow
                            else -> Color(0xFFFFC1CC) // Light Pink
                        }
                    } else Color.Transparent,
                    label = "tabBg"
                )
                val textCol by animateColorAsState(if (selected) Color.Black else Color.Gray, label = "tabText")

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(18.dp))
                        .background(bg)
                        .clickable { viewModel.onTabChange(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = textCol, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }

        // Content
        Box(modifier = Modifier.weight(1f)) {
            PatternLabContent(uiState, viewModel, onPickA, onPickB, onSaveClick)
        }
        
        Spacer(Modifier.height(100.dp))
    }
}

@Composable
fun PatternLabLandscape(
    uiState: PatternLabUiState,
    viewModel: PatternLabViewModel,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    onSaveClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Integrated Top Header
        PatternLabHeader(uiState, viewModel)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left Column: Tab Selector (Compact Vertical)
            Column(
                modifier = Modifier.width(160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("BASE A" to 0, "BASE B" to 1, "MIXER" to 2).forEach { (label, index) ->
                    val selected = uiState.selectedTab == index
                    val bg = if (selected) {
                        when (index) {
                            0 -> Color(0xFFB3E5FC)
                            1 -> Color(0xFFFFEB3B)
                            else -> Color(0xFFFFC1CC)
                        }
                    } else Color.Transparent

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .border(1.dp, if (selected) Color.Transparent else Color(0xFF222222), RoundedCornerShape(12.dp))
                            .clickable { viewModel.onTabChange(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.Black else Color.Gray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            // Right Area: Content (Scrollable)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
            ) {
                PatternLabContent(uiState, viewModel, onPickA, onPickB, onSaveClick)
            }
        }
    }
}

@Composable
private fun PatternLabHeader(uiState: PatternLabUiState, viewModel: PatternLabViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "PATTERN LAB",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
        )
        
        if (uiState.isPlaying) {
            IconButton(
                onClick = viewModel::togglePreview,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252))
            }
        } else if ((uiState.previewSteps.isNotEmpty() || uiState.selectedPlaylistA != null || uiState.selectedPlaylistB != null)) {
            IconButton(
                onClick = viewModel::togglePreview,
                modifier = Modifier.clip(CircleShape).background(Color(0x1A00C853))
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF00C853))
            }
        }
    }
}

@Composable
private fun PatternLabContent(
    uiState: PatternLabUiState,
    viewModel: PatternLabViewModel,
    onPickA: () -> Unit,
    onPickB: () -> Unit,
    onSaveClick: () -> Unit
) {
    when (uiState.selectedTab) {
        0 -> BaseTabContent(
            title = "SEQUENCE A",
            name = uiState.selectedPlaylistA?.playlist?.name,
            speed = uiState.speedMultiplierA,
            inverted = uiState.isInvertedA,
            mirrored = uiState.isMirroredA,
            reversed = uiState.isReversedA,
            pingPong = uiState.isPingPongA,
            onPick = onPickA,
            onSpeedChange = viewModel::onSpeedMultiplierAChange,
            onInvertChange = viewModel::onInvertAChange,
            onMirrorChange = viewModel::onMirrorAChange,
            onReverseChange = viewModel::onReverseAChange,
            onPingPongChange = viewModel::onPingPongAChange
        )
        1 -> BaseTabContent(
            title = "SEQUENCE B",
            name = uiState.selectedPlaylistB?.playlist?.name,
            speed = uiState.speedMultiplierB,
            inverted = uiState.isInvertedB,
            mirrored = uiState.isMirroredB,
            reversed = uiState.isReversedB,
            pingPong = uiState.isPingPongB,
            onPick = onPickB,
            onSpeedChange = viewModel::onSpeedMultiplierBChange,
            onInvertChange = viewModel::onInvertBChange,
            onMirrorChange = viewModel::onMirrorBChange,
            onReverseChange = viewModel::onReverseBChange,
            onPingPongChange = viewModel::onPingPongBChange
        )
        2 -> MixerTabContent(uiState, viewModel, onSaveClick = onSaveClick)
    }
}

@Composable
private fun BaseTabContent(
    title: String,
    name: String?,
    speed: Float,
    inverted: Boolean,
    mirrored: Boolean,
    reversed: Boolean,
    pingPong: Boolean,
    onPick: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onInvertChange: (Boolean) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    onReverseChange: (Boolean) -> Unit,
    onPingPongChange: (Boolean) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable { onPick() }) {
                        Text(title, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(name ?: "Select Sequence...", color = if (name != null) Color.White else Color(0xFF444444), fontWeight = FontWeight.Black, fontSize = 18.sp, maxLines = 1)
                    }
                    Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabToggleButton(icon = Icons.Default.InvertColors, label = "Invert", active = inverted) { onInvertChange(!inverted) }
                    LabToggleButton(icon = Icons.Default.Flip, label = "Mirror", active = mirrored) { onMirrorChange(!mirrored) }
                    LabToggleButton(icon = Icons.AutoMirrored.Filled.Undo, label = "Reverse", active = reversed) { onReverseChange(!reversed) }
                    LabToggleButton(icon = Icons.Default.SyncAlt, label = "Ping-Pong", active = pingPong) { onPingPongChange(!pingPong) }
                }
            }
        }

        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                LabSliderRow("Speed", "${"%.1f".format(speed)}x", speed, 0.5f..2.0f, onValueChange = onSpeedChange)
            }
        }
    }
}

@Composable
private fun MixerTabContent(uiState: PatternLabUiState, viewModel: PatternLabViewModel, onSaveClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("MERGE MODE", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ModeButton("Sequential", !uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(false) }
                ModeButton("Layered", uiState.isLayered, Modifier.weight(1f)) { viewModel.onMergeModeChange(true) }
            }
        }

        if (uiState.isLayered) {
            Surface(
                color = Color(0xFF111111),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFF222222))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("BLEND SETTINGS", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabBlendMode.entries.forEach { mode ->
                            ModeButton(
                                label = mode.name,
                                selected = uiState.blendMode == mode,
                                modifier = Modifier.weight(1f)
                            ) { viewModel.onBlendModeChange(mode) }
                        }
                    }

                    if (uiState.blendMode == LabBlendMode.CROSSFADE) {
                        LabSliderRow("Ratio", "${(uiState.crossfadeRatio * 100).toInt()}%", uiState.crossfadeRatio, 0f..1f, onValueChange = viewModel::onCrossfadeRatioChange)
                    }

                    LabSliderRow("Delay B", "${uiState.offsetB}ms", uiState.offsetB.toFloat(), 0f..1000f, steps = 20, onValueChange = { viewModel.onOffsetBChange(it.toInt()) })
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (uiState.previewSteps.isNotEmpty()) {
                Button(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("SAVE MIX", fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LabToggleButton(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) Color.White else Color(0xFF1A1A1A))
        ) {
            Icon(icon, null, tint = if (active) Color.Black else Color.Gray, modifier = Modifier.size(20.dp))
        }
        Text(label, color = if (active) Color.White else Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LabSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 15,
    onValueChange: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(modifier = Modifier.width(50.dp)) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(valueText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            steps = steps,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF222222)
            )
        )
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.height(48.dp).clickable { onClick() },
        color = if (selected) Color.White else Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = if (!selected) BorderStroke(1.dp, Color(0xFF222222)) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.Black else Color.Gray, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
    }
}

@Composable
private fun SaveMixDialog(
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Mix", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            TextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter mix name", color = Color.Gray) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF1A1A1A),
                    unfocusedContainerColor = Color(0xFF1A1A1A),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name) },
                enabled = name.isNotBlank()
            ) {
                Text("SAVE", color = if (name.isNotBlank()) Color(0xFF00C853) else Color.Gray, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.Gray) }
        },
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun PlaylistPickerDialog(
    playlists: List<PlaylistWithSteps>,
    onSelect: (PlaylistWithSteps) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Sequence", color = Color.White, fontWeight = FontWeight.Black) },
        text = {
            if (playlists.isEmpty()) {
                Text("No saved sequences found.", color = Color.Gray)
            } else {
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(playlists) { p ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(p) },
                            color = Color.Transparent
                        ) {
                            Text(p.playlist.name, modifier = Modifier.padding(16.dp), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL", color = Color.White, fontWeight = FontWeight.Black) }
        },
        containerColor = Color(0xFF111111),
        shape = RoundedCornerShape(28.dp)
    )
}
