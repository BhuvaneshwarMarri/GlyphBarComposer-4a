package com.smaarig.glyphbarcomposer.ui.studio

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.ui.studio.components.AnalysisOverlay
import com.smaarig.glyphbarcomposer.ui.studio.components.AnalyzerCard
import com.smaarig.glyphbarcomposer.ui.studio.components.ComposerPanel
import com.smaarig.glyphbarcomposer.ui.studio.components.ProjectSetupView
import com.smaarig.glyphbarcomposer.ui.studio.components.StudioHeader
import com.smaarig.glyphbarcomposer.ui.studio.components.StudioPlayerCard
import com.smaarig.glyphbarcomposer.ui.studio.components.TimelineCard
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

@Composable
fun MusicStudioScreen(
    viewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioPositionMs by viewModel.audioPositionMs.collectAsStateWithLifecycle()
    val composerIntensities by viewModel.composerIntensities.collectAsStateWithLifecycle()
    val liveGlyphIntensities by viewModel.liveGlyphIntensities.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    if (showSaveDialog) {
        var projectName by remember {
            mutableStateOf(
                uiState.audioName?.substringBeforeLast(".") ?: ""
            )
        }
        com.smaarig.glyphbarcomposer.ui.StyledSaveDialog(
            title = "Save Project",
            value = projectName,
            onValueChange = { projectName = it },
            onSave = {
                viewModel.saveMusicProject(projectName)
                showSaveDialog = false
            },
            onDismiss = { showSaveDialog = false },
            placeholder = "Project Name"
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

    Box(modifier = modifier
        .fillMaxSize()
        .background(Color.Black)) {
        AnimatedContent(
            targetState = uiState.audioUri == null,
            transitionSpec = {
                (fadeIn(animationSpec = tween(600, delayMillis = 100)) +
                        scaleIn(initialScale = 0.95f, animationSpec = tween(600)))
                    .togetherWith(
                        fadeOut(animationSpec = tween(400)) +
                                scaleOut(targetScale = 1.05f, animationSpec = tween(400))
                    )
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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            StudioHeader(
                                uiState,
                                viewModel,
                                onSaveClick = { showSaveDialog = true })

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
                            .background(Color.Black)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 24.dp),
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
