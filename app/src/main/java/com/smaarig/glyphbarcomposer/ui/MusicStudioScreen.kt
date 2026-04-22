package com.smaarig.glyphbarcomposer.ui

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.data.MusicStudioEvent
import com.smaarig.glyphbarcomposer.ui.studio.rememberStudioTimelineState
import com.smaarig.glyphbarcomposer.ui.viewmodel.BeatAlgorithm
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

private val TRACK_LABEL_WIDTH = 48.dp
private val TRACK_HEIGHT = 180.dp
private val WAVEFORM_HEIGHT = 48.dp
private val RESIZE_HANDLE_WIDTH: Dp = 14.dp
private val MIN_BLOCK_WIDTH: Dp = 24.dp

@Composable
fun MusicStudioScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val visualizerData by viewModel.visualizerData.collectAsStateWithLifecycle()
    val audioPositionMs by viewModel.audioPositionMs.collectAsStateWithLifecycle()
    val composerIntensities by viewModel.composerIntensities.collectAsStateWithLifecycle()
    val liveGlyphIntensities by viewModel.liveGlyphIntensities.collectAsStateWithLifecycle()
    
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    if (showSaveDialog) {
        var projectName by remember { mutableStateOf(uiState.audioName?.substringBeforeLast(".") ?: "") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Project", color = Color.White, fontWeight = FontWeight.Black) },
            text = {
                TextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    placeholder = { Text("Project Name", color = Color.Gray) },
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
                TextButton(onClick = {
                    viewModel.saveMusicProject(projectName)
                    showSaveDialog = false
                }) {
                    Text("SAVE", color = Color(0xFF00C853), fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(28.dp)
        )
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasPermission = granted
        if (granted) viewModel.retryVisualizerSetup()
    }
    LaunchedEffect(hasPermission) {
        if (hasPermission && uiState.isAudioPlaying) viewModel.retryVisualizerSetup()
    }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { u ->
            if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            val name = context.contentResolver.query(u, null, null, null, null)?.use { cur ->
                if (cur.moveToFirst()) {
                    val idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cur.getString(idx) else "Unknown Song"
                } else "Unknown Song"
            } ?: "Unknown Song"
            viewModel.loadSong(u, name)
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        if (uiState.audioUri == null) {
            ProjectSetupView(
                uiState = uiState,
                onPickFile = { fileLauncher.launch("audio/*") },
                onAlgorithmSelect = { viewModel.setAlgorithm(it) },
                onToggleRedGlyph = { viewModel.toggleRedGlyph(it) },
                onBpmChange = viewModel::setBpmOverride
            )
        } else {
            if (isLandscape) {
                // Split-pane Landscape Layout
                Row(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Left Pane: Info & Audio Controls (Scrollable)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        StudioHeader(uiState, viewModel, onSaveClick = { showSaveDialog = true })
                        
                        StudioPlayerCard(
                            uiState = uiState,
                            audioPositionMs = audioPositionMs,
                            onPlayPause = viewModel::toggleMusicPlayback,
                            onSeek = viewModel::seekMusic,
                            onChangeAudio = { fileLauncher.launch("audio/*") }
                        )

                        AnalyzerCard(uiState, visualizerData, viewModel)
                    }

                    // Right Pane: Editor & Composer (Scrollable)
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        TimelineCard(uiState, audioPositionMs, viewModel)
                        
                        ComposerPanel(
                            intensities = composerIntensities,
                            liveIntensities = liveGlyphIntensities,
                            isPlaying = uiState.isAudioPlaying,
                            isReady = uiState.isAnalysisComplete,
                            isSelected = uiState.selectedEventId != null,
                            defaultDuration = uiState.defaultDurationMs,
                            onIntensityChange = viewModel::onComposerIntensityChange,
                            onDurationChange = viewModel::setDefaultDuration,
                            onClear = viewModel::clearComposer,
                            onInsert = viewModel::addMusicEvent
                        )
                        
                        Spacer(Modifier.height(80.dp))
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    StudioHeader(uiState, viewModel, onSaveClick = { showSaveDialog = true })

                    StudioPlayerCard(
                        uiState = uiState,
                        audioPositionMs = audioPositionMs,
                        onPlayPause = viewModel::toggleMusicPlayback,
                        onSeek = viewModel::seekMusic,
                        onChangeAudio = { fileLauncher.launch("audio/*") }
                    )

                    AnalyzerCard(uiState, visualizerData, viewModel)
                    
                    TimelineCard(uiState, audioPositionMs, viewModel)

                    ComposerPanel(
                        intensities = composerIntensities,
                        liveIntensities = liveGlyphIntensities,
                        isPlaying = uiState.isAudioPlaying,
                        isReady = uiState.isAnalysisComplete,
                        isSelected = uiState.selectedEventId != null,
                        defaultDuration = uiState.defaultDurationMs,
                        onIntensityChange = viewModel::onComposerIntensityChange,
                        onDurationChange = viewModel::setDefaultDuration,
                        onClear = viewModel::clearComposer,
                        onInsert = viewModel::addMusicEvent
                    )
                    
                    Spacer(Modifier.height(120.dp))
                }
            }
        }

        AnimatedVisibility(visible = uiState.isAnalyzing, enter = fadeIn(), exit = fadeOut()) {
            AnalysisOverlay(uiState.selectedAlgorithm, uiState.isAnalysisComplete)
        }
    }
}

@Composable
private fun StudioHeader(
    uiState: MusicStudioUiState, 
    viewModel: MusicStudioViewModel,
    onSaveClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "GLYPH STUDIO",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
            )
            Text(
                "Sync patterns to audio",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (uiState.musicEvents.isNotEmpty()) {
                IconButton(
                    onClick = { if (!uiState.showSaveSuccess && !uiState.isSaving) onSaveClick() },
                    modifier = Modifier.clip(CircleShape).background(
                        if (uiState.showSaveSuccess) Color(0xFF00C853) else Color(0x1A00C853)
                    )
                ) {
                    AnimatedContent(
                        targetState = uiState.showSaveSuccess,
                        label = "saveIcon"
                    ) { success ->
                        if (success) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF00C853), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Save, null, tint = Color(0xFF00C853), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            IconButton(
                onClick = viewModel::clearAllMusicEvents,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(Icons.Default.DeleteSweep, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun AnalyzerCard(uiState: MusicStudioUiState, visualizerData: List<Float>, viewModel: MusicStudioViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SYNC ENGINE", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Surface(
            color = Color(0xFF111111),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                FrequencyBar(visualizerData)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Algorithm Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            onClick = { expanded = true },
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    uiState.selectedAlgorithm.displayName,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        ) {
                            BeatAlgorithm.entries.forEach { algo ->
                                DropdownMenuItem(
                                    text = { Text(algo.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setAlgorithm(algo)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 2. Red Toggle (Mini)
                    Box(
                        modifier = Modifier
                            .size(height = 42.dp, width = 56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (uiState.includeRedGlyph) Color(0xFFFF1744).copy(0.15f) else Color(0xFF1A1A1A))
                            .border(1.dp, if (uiState.includeRedGlyph) Color(0xFFFF1744).copy(0.5f) else Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                            .clickable { viewModel.toggleRedGlyph(!uiState.includeRedGlyph) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Adjust, 
                            null, 
                            tint = if (uiState.includeRedGlyph) Color(0xFFFF1744) else Color(0xFF444444),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // 3. Generate Button
                    Button(
                        onClick = viewModel::reanalyze,
                        enabled = !uiState.isAnalyzing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF1A1A1A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        if (uiState.isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Text("GENERATE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }

                if (uiState.selectedAlgorithm == BeatAlgorithm.BPM_GRID) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF080808))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tempo", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.setBpmOverride(uiState.bpmOverride - 5) }, modifier = Modifier.size(32.dp)) {
                                Text("−", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                            Text("${uiState.bpmOverride} BPM", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 12.dp))
                            IconButton(onClick = { viewModel.setBpmOverride(uiState.bpmOverride + 5) }, modifier = Modifier.size(32.dp)) {
                                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineCard(uiState: MusicStudioUiState, audioPositionMs: Int, viewModel: MusicStudioViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("TIMELINE", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        StudioTimelineEditor(
            uiState = uiState,
            audioPositionMs = audioPositionMs,
            viewModel = viewModel,
            modifier = Modifier.height(280.dp)
        )
    }
}

@Composable
private fun StudioPlayerCard(
    uiState: MusicStudioUiState,
    audioPositionMs: Int,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onChangeAudio: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Rotating Vinyl
                val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f, targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(4000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ), label = "rotation"
                )
                
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer { if (uiState.isAudioPlaying) rotationZ = rotation }
                        .clip(CircleShape)
                        .background(Color(0xFF080808))
                        .border(1.5.dp, Color(0xFF333333), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(40.dp)) {
                        val r = size.minDimension / 2
                        drawCircle(Color(0xFF1A1A1A), radius = r, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                        drawCircle(Color(0xFF1A1A1A), radius = r * 0.6f, style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
                    }
                    Icon(
                        Icons.Default.MusicNote, null,
                        tint = if (uiState.isAudioPlaying) Color(0xFF00C853) else Color(0xFF555555),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        uiState.audioName ?: "Select a Track",
                        color = Color.White, fontWeight = FontWeight.Black, fontSize = 17.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${formatTime(audioPositionMs)} / ${formatTime(uiState.audioDurationMs)}",
                        color = Color.Gray, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                IconButton(
                    onClick = onChangeAudio,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF1A1A1A))
                ) {
                    Icon(Icons.Default.FolderOpen, null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Slider(
                    value = audioPositionMs.toFloat(),
                    onValueChange = onSeek,
                    valueRange = 0f..uiState.audioDurationMs.toFloat().coerceAtLeast(1f),
                    enabled = uiState.isAnalysisComplete,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color(0xFF222222)
                    ),
                    modifier = Modifier.weight(1f).height(24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isAnalysisComplete) Color.White else Color(0xFF1A1A1A))
                        .clickable(enabled = uiState.isAnalysisComplete) { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (uiState.isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = if (uiState.isAnalysisComplete) Color.Black else Color(0xFF333333),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FrequencyBar(magnitudes: List<Float>, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF080808))
            .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        magnitudes.forEachIndexed { i, m ->
            var lastM by remember { mutableFloatStateOf(0f) }
            val isRising = m > lastM
            
            val h by animateFloatAsState(
                targetValue = (m / 255f).coerceIn(0.04f, 1f),
                animationSpec = if (isRising) {
                    tween(80, easing = FastOutLinearInEasing)
                } else {
                    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
                },
                label = "fq_$i"
            )
            
            SideEffect { lastM = m }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = when {
                                m > 200f -> listOf(Color(0xFFFF1744), Color(0xFFFF5252)) // Red/Clipping
                                m > 150f -> listOf(Color(0xFFFFFF00), Color(0xFFFFEA00)) // Yellow/Peak
                                m > 40f -> listOf(Color(0xFFFFD600), Color(0xFFFFC400))  // Yellow/Normal
                                else -> listOf(Color(0xFF1A1A1A), Color(0xFF0F0F0F))     // Dim/Idle
                            }
                        )
                    )
            )
        }
    }
}


@Composable
private fun StudioTimelineEditor(
    uiState: MusicStudioUiState,
    audioPositionMs: Int,
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val timelineState = rememberStudioTimelineState()

    LaunchedEffect(audioPositionMs, uiState.isAudioPlaying) {
        if (!uiState.isAudioPlaying) return@LaunchedEffect
        val px = timelineState.timeToPx(audioPositionMs.toLong())
        val start = timelineState.horizontalScrollState.value
        val width = timelineState.horizontalScrollState.viewportSize
        if (px > start + width * 0.70f || px < start + width * 0.10f) {
            timelineState.horizontalScrollState.animateScrollTo(
                (px - width * 0.25f).toInt().coerceAtLeast(0),
                animationSpec = tween(400, easing = FastOutSlowInEasing)
            )
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFF080808),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF1A1A1A))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Vertical track labels
            Row(
                modifier = Modifier
                    .width(TRACK_LABEL_WIDTH)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0C0C))
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.height(WAVEFORM_HEIGHT).fillMaxWidth(),
                        contentAlignment = Alignment.Center) {
                        Text("WAVE", color = Color(0xFF222222), fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("PATTERNS", color = Color(0xFF333333), fontSize = 9.sp, fontWeight = FontWeight.Black,
                            modifier = Modifier.graphicsLayer(rotationZ = -90f))
                    }
                }
                VerticalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
            }

            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .horizontalScroll(timelineState.horizontalScrollState)
                    .verticalScroll(timelineState.verticalScrollState)
            ) {
                val totalWidth = timelineState.timeToDp(uiState.audioDurationMs.toLong())
                Box(modifier = Modifier.width(totalWidth + 160.dp).fillMaxHeight()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxWidth().height(WAVEFORM_HEIGHT).background(Color(0xFF060606))) {
                            WaveformRuler(uiState.waveform, Modifier.fillMaxSize())
                        }
                        HorizontalDivider(color = Color(0xFF1A1A1A), thickness = 1.dp)
                        
                        SequenceTrack(
                            events = uiState.musicEvents,
                            state = timelineState,
                            totalDurationMs = uiState.audioDurationMs,
                            selectedEventId = uiState.selectedEventId,
                            onSelectEvent = viewModel::selectEvent,
                            onMoveEvent = viewModel::updateEventPosition,
                            onResizeEvent = viewModel::updateEventStartAndDuration,
                            onDeleteEvent = viewModel::deleteMusicEvent,
                            modifier = Modifier.fillMaxWidth().height(TRACK_HEIGHT)
                        )
                    }

                    // Playhead
                    val phX = timelineState.timeToDp(audioPositionMs.toLong())
                    Box(
                        modifier = Modifier.offset(x = phX).width(2.dp).fillMaxHeight()
                            .background(Color(0xFF00C853).copy(0.8f))
                            .pointerInput(Unit) {
                                detectDragGestures { change, drag ->
                                    change.consume()
                                    viewModel.seekMusic((audioPositionMs + timelineState.dragDeltaToMs(drag.x)).toFloat())
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier.align(Alignment.TopCenter).size(16.dp)
                                .offset(y = (-2).dp).clip(CircleShape)
                                .background(Color(0xFF00C853))
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WaveformRuler(
    waveform: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val samples = waveform.size
        if (samples == 0) return@Canvas

        val pxPerSample = width / samples
        
        // Background subtle glow
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF00C853).copy(alpha = 0.05f), Color.Transparent, Color(0xFF00C853).copy(alpha = 0.05f))
            ),
            size = size
        )

        waveform.forEachIndexed { i, energy ->
            val x = i * pxPerSample
            val barHeight = (energy * height * 0.8f).coerceAtLeast(2f)
            
            // Draw dual-sided bars for a more premium "pro audio" look
            drawLine(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00E676).copy(alpha = 0.8f),
                        Color(0xFF00C853),
                        Color(0xFF00E676).copy(alpha = 0.8f)
                    )
                ),
                start = Offset(x, height / 2 - barHeight / 2),
                end = Offset(x, height / 2 + barHeight / 2),
                strokeWidth = (pxPerSample * 0.7f).coerceAtLeast(1.5f),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SequenceTrack(
    events: List<MusicStudioEvent>,
    state: com.smaarig.glyphbarcomposer.ui.studio.StudioTimelineState,
    totalDurationMs: Int,
    selectedEventId: Long?,
    onSelectEvent: (MusicStudioEvent?) -> Unit,
    onMoveEvent: (MusicStudioEvent, Long) -> Unit,
    onResizeEvent: (MusicStudioEvent, Long, Int) -> Unit,
    onDeleteEvent: (MusicStudioEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .pointerInput(state) {
                detectTapGestures { onSelectEvent(null) }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val msPerGrid = 500L
            val pxPerGrid = state.timeToPx(msPerGrid)
            val count = (totalDurationMs / msPerGrid).toInt()
            for (i in 0..count) {
                val x = i * pxPerGrid
                drawLine(Color(0xFF151515), Offset(x, 0f), Offset(x, size.height), 1f)
            }
        }

        events.forEach { event ->
            key(event.id) {
                SequenceBlock(
                    event = event,
                    state = state,
                    isSelected = event.id == selectedEventId,
                    onClick = { onSelectEvent(event) },
                    onMove = { delta -> onMoveEvent(event, (event.timestampMs + delta).coerceAtLeast(0L)) },
                    onResizeLeft = { delta ->
                        val start = (event.timestampMs + delta).coerceAtLeast(0L)
                        val dur = (event.timestampMs + event.durationMs - start).toInt().coerceAtLeast(50)
                        onResizeEvent(event, start, dur)
                    },
                    onResizeRight = { delta ->
                        onResizeEvent(event, event.timestampMs, (event.durationMs + delta).toInt().coerceAtLeast(50))
                    },
                    onDelete = { onDeleteEvent(event) }
                )
            }
        }
    }
}

@Composable
private fun SequenceBlock(
    event: MusicStudioEvent,
    state: com.smaarig.glyphbarcomposer.ui.studio.StudioTimelineState,
    isSelected: Boolean,
    onClick: () -> Unit,
    onMove: (Long) -> Unit,
    onResizeLeft: (Long) -> Unit,
    onResizeRight: (Long) -> Unit,
    onDelete: () -> Unit
) {
    var moveDeltaMs by remember(event.id) { mutableStateOf(0L) }
    var resizeLeftDeltaMs by remember(event.id) { mutableStateOf(0L) }
    var resizeRightDeltaMs by remember(event.id) { mutableStateOf(0L) }
    var isDragging by remember(event.id) { mutableStateOf(false) }
    var isResizing by remember(event.id) { mutableStateOf(false) }

    val currentTimestampMs = (event.timestampMs + moveDeltaMs + resizeLeftDeltaMs).coerceAtLeast(0L)
    val currentDurationMs = (event.durationMs + resizeRightDeltaMs - resizeLeftDeltaMs).coerceAtLeast(50)

    val x = state.timeToDp(currentTimestampMs)
    val widthDp = state.timeToDp(currentDurationMs).coerceAtLeast(MIN_BLOCK_WIDTH)

    Box(
        modifier = Modifier
            .offset(x = x)
            .width(widthDp)
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .graphicsLayer {
                if (isDragging || isResizing) {
                    scaleX = 1.02f
                    scaleY = 1.05f
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = RESIZE_HANDLE_WIDTH)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) Color(0xFF00C853).copy(0.2f) else Color(0xFF161616))
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) Color(0xFF00C853) else Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(8.dp)
                )
                .pointerInput(event.id) {
                    detectTapGestures(
                        onTap = { onClick() },
                        onLongPress = { onDelete() }
                    )
                }
                .pointerInput(event.id, state) {
                    detectDragGestures(
                        onDragStart = { isDragging = true; onClick() },
                        onDragEnd = {
                            isDragging = false
                            val totalDelta = moveDeltaMs
                            val snappedDelta = Math.round(totalDelta / 25.0) * 25
                            onMove(snappedDelta)
                            moveDeltaMs = 0L
                        },
                        onDragCancel = { isDragging = false; moveDeltaMs = 0L }
                    ) { change, drag ->
                        change.consume()
                        moveDeltaMs += state.dragDeltaToMs(drag.x)
                    }
                }
        ) {
            if (isDragging) Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.05f)))
            
            Column(
                modifier = Modifier.align(Alignment.Center).padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(1.5.dp)
            ) {
                repeat(7) { i -> PatternDot(event, i) }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(RESIZE_HANDLE_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                .background(Color.White.copy(0.05f))
                .pointerInput(event.id, state) {
                    detectDragGestures(
                        onDragStart = { isResizing = true; onClick() },
                        onDragEnd = { 
                            isResizing = false
                            val snapped = Math.round(resizeLeftDeltaMs / 25.0) * 25
                            onResizeLeft(snapped)
                            resizeLeftDeltaMs = 0L 
                        },
                        onDragCancel = { isResizing = false; resizeLeftDeltaMs = 0L }
                    ) { change, drag ->
                        change.consume()
                        resizeLeftDeltaMs += state.dragDeltaToMs(drag.x)
                    }
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(RESIZE_HANDLE_WIDTH)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                .background(Color.White.copy(0.05f))
                .pointerInput(event.id, state) {
                    detectDragGestures(
                        onDragStart = { isResizing = true; onClick() },
                        onDragEnd = { 
                            isResizing = false
                            val snapped = Math.round(resizeRightDeltaMs / 25.0) * 25
                            onResizeRight(snapped)
                            resizeRightDeltaMs = 0L 
                        },
                        onDragCancel = { isResizing = false; resizeRightDeltaMs = 0L }
                    ) { change, drag ->
                        change.consume()
                        resizeRightDeltaMs += state.dragDeltaToMs(drag.x)
                    }
                }
        )
    }
}

@Composable
private fun PatternDot(event: MusicStudioEvent, index: Int) {
    val ch = getChannelForIndex(index)
    val intensity = event.channelIntensities[ch] ?: 0
    val isRed = index == 6
    Box(
        modifier = Modifier
            .size(if (intensity > 0) 4.dp else 2.dp)
            .clip(CircleShape)
            .background(
                if (intensity > 0) {
                    if (isRed) Color(0xFFFF1744) else Color.White.copy(alpha = (intensity / 3f).coerceIn(0.4f, 1.0f))
                } else Color(0xFF2A2A2A)
            )
    )
}

@Composable
private fun ComposerPanel(
    intensities: List<Int>,
    liveIntensities: List<Int>,
    isPlaying: Boolean,
    isReady: Boolean,
    isSelected: Boolean,
    defaultDuration: Int,
    onIntensityChange: (Int, Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onClear: () -> Unit,
    onInsert: () -> Unit
) {
    val anyActive = intensities.any { it > 0 }
    val canInsert = isReady && !isPlaying && anyActive
    val isEditable = !isPlaying || isSelected

    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        if (isPlaying && !isSelected) "LIVE OUTPUT" else "GLYPH PAINTER",
                        color = if (isPlaying && !isSelected) Color(0xFF00C853) else Color.White,
                        fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp
                    )
                    Text(
                        if (isPlaying && !isSelected) "Responding in real-time"
                        else "Paint pattern & insert beat",
                        color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (anyActive && !isPlaying) {
                        IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            GlyphComposerPanel(
                intensities = if (isPlaying && !isSelected) liveIntensities else intensities,
                onIntensityChange = onIntensityChange,
                enabled = isEditable,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Duration Dropdown/Selector simplified
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .clickable { 
                            val next = when(defaultDuration) {
                                100 -> 200; 200 -> 300; 300 -> 500; 500 -> 800; else -> 100
                            }
                            onDurationChange(next)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("${defaultDuration}ms", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = onInsert,
                    enabled = canInsert,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black,
                        disabledContainerColor = Color(0xFF1A1A1A), disabledContentColor = Color(0xFF333333)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("INSERT", fontSize = 13.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun GlyphComposerPanel(
    intensities: List<Int>,
    onIntensityChange: (index: Int, newIntensity: Int) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF080808))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        intensities.forEachIndexed { idx, intensity ->
            val color = when (idx) {
                0 -> Color(0xFFFFFFFF)
                1 -> Color(0xFF82AAFF)
                2 -> Color(0xFF89DDFF)
                3 -> Color(0xFFC3E88D)
                4 -> Color(0xFFFFCB6B)
                5 -> Color(0xFFB39DDB)
                else -> Color(0xFFFF1744)
            }
            if (idx < 6) {
                IntensityFader(
                    color = color,
                    intensity = intensity,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onIntensityChange = { onIntensityChange(idx, it) }
                )
            } else {
                RedFader(
                    color = color,
                    isOn = intensity > 0,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    onToggle = { onIntensityChange(idx, if (it) 3 else 0) }
                )
            }
        }
    }
}

@Composable
private fun IntensityFader(
    color: Color,
    intensity: Int,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onIntensityChange: (Int) -> Unit
) {
    var trackHeightPx by remember { mutableStateOf(0f) }

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F0F0F))
                .border(1.dp, Color(0xFF1A1A1A), RoundedCornerShape(12.dp))
                .onGloballyPositioned { trackHeightPx = it.size.height.toFloat() }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onTap = { offset ->
                            val fraction = 1f - (offset.y / trackHeightPx).coerceIn(0f, 1f)
                            val newLevel = when {
                                fraction > 0.8f -> 3
                                fraction > 0.45f -> 2
                                fraction > 0.15f -> 1
                                else -> 0
                            }
                            onIntensityChange(newLevel)
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectDragGestures { change, _ ->
                        change.consume()
                        val y = change.position.y
                        val fraction = 1f - (y / trackHeightPx).coerceIn(0f, 1f)
                        val newLevel = when {
                            fraction > 0.8f -> 3
                            fraction > 0.45f -> 2
                            fraction > 0.15f -> 1
                            else -> 0
                        }
                        if (newLevel != intensity) onIntensityChange(newLevel)
                    }
                }
        ) {
            val fillFraction by animateFloatAsState(
                targetValue = intensity / 3f,
                animationSpec = spring(stiffness = Spring.StiffnessLow)
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(fillFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.4f), color)
                        )
                    )
                    .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
            )

            // Visual markers
            Column(
                modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(4) { i ->
                    Box(
                        Modifier
                            .width(8.dp)
                            .height(1.dp)
                            .background(if ((3-i) <= intensity && intensity > 0) Color.White.copy(0.4f) else Color.White.copy(0.05f))
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (intensity > 0) color else Color(0xFF1A1A1A))
        )
    }
}

@Composable
private fun RedFader(
    color: Color,
    isOn: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val bgAlpha by animateFloatAsState(if (isOn) 0.3f else 0f)
        val borderAlpha by animateFloatAsState(if (isOn) 1f else 0.1f)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(color.copy(alpha = bgAlpha))
                .border(
                    width = 1.dp,
                    color = color.copy(alpha = borderAlpha),
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { onToggle(!isOn) },
            contentAlignment = Alignment.Center
        ) {
            if (isOn) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = "RED",
            color = if (isOn) color else Color(0xFF444444),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
private fun AnalysisOverlay(algo: BeatAlgorithm, complete: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(24.dp)) {
            if (complete) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(64.dp))
                Text("ANALYSIS COMPLETE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp)
                Text("Your track is ready for synchronization", color = Color(0xFF666666), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                val squares = List(7) { i ->
                    rememberInfiniteTransition(label = "sq$i").animateFloat(
                        initialValue = 0.1f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500, delayMillis = i * 100), RepeatMode.Reverse),
                        label = "a$i"
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    squares.forEachIndexed { i, alpha ->
                        Box(
                            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(4.dp))
                                .background((if (i == 6) Color(0xFFFF1744) else Color.White).copy(alpha = alpha.value))
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ANALYZING WITH ${algo.displayName.uppercase()}", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.sp)
                    if (algo == BeatAlgorithm.MANUAL_EDIT) {
                        Text(algo.description, color = Color(0xFF444444), fontSize = 12.sp, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 50.dp), fontWeight = FontWeight.Bold)
                    }
                }
                
                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Color(0xFF00C853), strokeWidth = 3.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectSetupView(
    uiState: MusicStudioUiState,
    onPickFile: () -> Unit,
    onAlgorithmSelect: (BeatAlgorithm) -> Unit,
    onToggleRedGlyph: (Boolean) -> Unit,
    onBpmChange: (Int) -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
            .verticalScroll(rememberScrollState())
            .padding(if (isLandscape) 24.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = if (isLandscape) Arrangement.Top else Arrangement.Center
    ) {
        if (!isLandscape) {
            StudioLogoSection()
            Spacer(Modifier.height(40.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "GLYPH STUDIO",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    letterSpacing = 2.sp,
                    fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                )
                Button(
                    onClick = onPickFile,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AudioFile, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SELECT TRACK", fontWeight = FontWeight.Black, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(if (isLandscape) 1f else 1f),
            color = Color(0xFF111111),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF222222))
        ) {
            Column(modifier = Modifier.padding(if (isLandscape) 24.dp else 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "SYNC ENGINE",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Select detection logic",
                            color = Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { expanded = true },
                            color = Color(0xFF1A1A1A),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF2A2A2A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    uiState.selectedAlgorithm.displayName.uppercase(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color(0xFF1A1A1A)).border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                        ) {
                            BeatAlgorithm.entries.forEach { algo ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(algo.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(algo.description, color = Color.Gray, fontSize = 10.sp, lineHeight = 14.sp)
                                        }
                                    },
                                    onClick = {
                                        onAlgorithmSelect(algo)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (uiState.selectedAlgorithm == BeatAlgorithm.BPM_GRID) {
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF080808))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Target Tempo", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onBpmChange(uiState.bpmOverride - 5) }) {
                                Text("−", color = Color.White, fontSize = 20.sp)
                            }
                            Text("${uiState.bpmOverride} BPM", color = Color.White, fontWeight = FontWeight.Black)
                            IconButton(onClick = { onBpmChange(uiState.bpmOverride + 5) }) {
                                Text("+", color = Color.White, fontSize = 20.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A1A))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (uiState.includeRedGlyph) Color(0xFFFF1744).copy(0.2f) else Color(0xFF080808)),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (uiState.includeRedGlyph) Color(0xFFFF1744) else Color(0xFF333333)))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("RED GLYPH SYNC", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                            Text("Center LED interaction", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Switch(
                        checked = uiState.includeRedGlyph,
                        onCheckedChange = onToggleRedGlyph,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFFF1744),
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = Color(0xFF080808)
                        )
                    )
                }
            }
        }

        if (!isLandscape) {
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onPickFile,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(64.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Icon(Icons.Default.AudioFile, null, Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Text("PICK AUDIO FILE", fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp)
            }
        }
        
        Spacer(Modifier.height(120.dp))
    }
}

@Composable
private fun StudioLogoSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(120.dp)
                .clip(CircleShape).background(Color(0xFF0F0F0F))
                .border(1.dp, Color(0xFF222222), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            repeat(7) { i ->
                val angle = i * (360f / 7f)
                val r = 38.dp
                Box(
                    modifier = Modifier.offset(
                        x = (r.value * kotlin.math.cos(Math.toRadians(angle.toDouble()))).dp,
                        y = (r.value * kotlin.math.sin(Math.toRadians(angle.toDouble()))).dp
                    ).size(8.dp).clip(CircleShape)
                        .background(if (i == 6) Color(0xFFFF1744) else Color.White.copy(alpha = 0.4f))
                )
            }
            Icon(Icons.Default.GraphicEq, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }

        Spacer(Modifier.height(32.dp))
        Text("GLYPH STUDIO", color = Color.White, fontWeight = FontWeight.Black,
            fontSize = 28.sp, letterSpacing = 4.sp, fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont)
        Spacer(Modifier.height(8.dp))
        Text("Precision audio synchronization",
            color = Color(0xFF555555), fontSize = 13.sp, textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold)
    }
}


private fun formatTime(ms: Int): String {
    val total = ms / 1000
    return "%02d:%02d".format(total / 60, total % 60)
}
