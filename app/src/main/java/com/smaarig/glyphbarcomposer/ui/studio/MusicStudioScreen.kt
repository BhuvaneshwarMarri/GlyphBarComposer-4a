package com.smaarig.glyphbarcomposer.ui.studio

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.ui.studio.components.*
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

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
        AnimatedContent(
            targetState = uiState.audioUri == null,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600, delayMillis = 100)) + 
                 scaleIn(initialScale = 0.95f, animationSpec = tween(600)))
                    .togetherWith(fadeOut(animationSpec = tween(400)) + 
                                  scaleOut(targetScale = 1.05f, animationSpec = tween(400)))
            },
            label = "StudioContentTransition"
        ) { isSetup ->
            if (isSetup) {
                ProjectSetupView(
                    uiState = uiState,
                    onPickFile = { fileLauncher.launch("audio/*") },
                    onAlgorithmSelect = { viewModel.setAlgorithm(it) },
                    onToggleRedGlyph = { viewModel.toggleRedGlyph(it) },
                    onBpmChange = viewModel::setBpmOverride
                )
            } else {
                if (isLandscape) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
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

                            AnalyzerCard(uiState, viewModel)
                        }

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

                        AnalyzerCard(uiState, viewModel)
                        
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
        }

        AnimatedVisibility(visible = uiState.isAnalyzing, enter = fadeIn(), exit = fadeOut()) {
            AnalysisOverlay(uiState.selectedAlgorithm, uiState.isAnalysisComplete)
        }
    }
}
