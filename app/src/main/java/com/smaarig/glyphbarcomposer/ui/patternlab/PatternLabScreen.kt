package com.smaarig.glyphbarcomposer.ui.patternlab

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.ui.patternlab.components.*
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.PatternLabViewModel

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
        PatternLabHeader(uiState, viewModel)

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
                            0 -> Color(0xFF0086EA)
                            1 -> Color(0xFFFFEB3B)
                            else -> Color(0xFFFFC1CC)
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
        PatternLabHeader(uiState, viewModel)

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(
                modifier = Modifier.width(160.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("BASE A" to 0, "BASE B" to 1, "MIXER" to 2).forEach { (label, index) ->
                    val selected = uiState.selectedTab == index
                    val bg = if (selected) {
                        when (index) {
                            0 -> Color(0xFF0086EA)
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
                            color = if (selected) Color.Black else Color(0xFF555555),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

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
