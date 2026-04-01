package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.LibraryViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicSyncViewModel

// ─── Constants ──────────────────────────────────────────────────────────────
private val ch = listOf(
    Glyph.Code_25111.A_1,
    Glyph.Code_25111.A_2,
    Glyph.Code_25111.A_3,
    Glyph.Code_25111.A_4,
    Glyph.Code_25111.A_5,
    Glyph.Code_25111.A_6
)

private val libIntensityColor = listOf(
    Color(0xFF1C1C1C),
    Color(0xFF686868),
    Color(0xFFCDCDCD),
    Color(0xFFFFFFFF),
)

// ─── Preset content ─────────────────────────────────────────────────────────
private data class PresetSequence(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val steps: List<GlyphSequence>
)

private val presetSequences: List<PresetSequence> by lazy {
    listOf(
        PresetSequence(
            name = "PULSE",
            description = "All channels breathe in unison",
            icon = Icons.Default.FavoriteBorder,
            steps = listOf(
                GlyphSequence(ch.associateWith { 1 }, 400),
                GlyphSequence(ch.associateWith { 2 }, 400),
                GlyphSequence(ch.associateWith { 3 }, 400),
                GlyphSequence(ch.associateWith { 2 }, 400),
                GlyphSequence(ch.associateWith { 1 }, 400),
                GlyphSequence(ch.associateWith { 0 }, 200),
            )
        ),
        PresetSequence(
            name = "WAVE",
            description = "Smooth sweep A1→A6",
            icon = Icons.Default.Waves,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 1), 200),
                GlyphSequence(mapOf(ch[0] to 2, ch[1] to 3, ch[2] to 1), 200),
                GlyphSequence(mapOf(ch[1] to 2, ch[2] to 3, ch[3] to 1), 200),
                GlyphSequence(mapOf(ch[2] to 2, ch[3] to 3, ch[4] to 1), 200),
                GlyphSequence(mapOf(ch[3] to 2, ch[4] to 3, ch[5] to 1), 200),
                GlyphSequence(mapOf(ch[4] to 2, ch[5] to 3), 200),
                GlyphSequence(mapOf(ch[5] to 2), 200),
                GlyphSequence(ch.associateWith { 0 }, 100),
            )
        ),
        PresetSequence(
            name = "WINGS",
            description = "Symmetric dual-end pulse",
            icon = Icons.Default.FlightTakeoff,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 1, ch[5] to 1), 300),
                GlyphSequence(mapOf(ch[0] to 3, ch[5] to 3, ch[1] to 1, ch[4] to 1), 300),
                GlyphSequence(mapOf(ch[0] to 2, ch[5] to 2, ch[1] to 3, ch[4] to 3, ch[2] to 1, ch[3] to 1), 300),
                GlyphSequence(mapOf(ch[0] to 1, ch[5] to 1, ch[1] to 2, ch[4] to 2, ch[2] to 3, ch[3] to 3), 300),
                GlyphSequence(mapOf(ch[2] to 2, ch[3] to 2), 300),
                GlyphSequence(ch.associateWith { 0 }, 150),
            )
        ),
        PresetSequence(
            name = "STROBE",
            description = "Fast high-frequency flash",
            icon = Icons.Default.FlashOn,
            steps = listOf(
                GlyphSequence(ch.associateWith { 3 }, 80),
                GlyphSequence(ch.associateWith { 0 }, 80),
                GlyphSequence(ch.associateWith { 3 }, 80),
                GlyphSequence(ch.associateWith { 0 }, 80),
                GlyphSequence(ch.associateWith { 3 }, 80),
                GlyphSequence(ch.associateWith { 0 }, 200),
            )
        ),
        PresetSequence(
            name = "BREATHE",
            description = "Slow intensity ramp up/down",
            icon = Icons.Default.Air,
            steps = listOf(
                GlyphSequence(ch.associateWith { 0 }, 200),
                GlyphSequence(ch.associateWith { 1 }, 600),
                GlyphSequence(ch.associateWith { 2 }, 600),
                GlyphSequence(ch.associateWith { 3 }, 800),
                GlyphSequence(ch.associateWith { 2 }, 600),
                GlyphSequence(ch.associateWith { 1 }, 600),
                GlyphSequence(ch.associateWith { 0 }, 400),
            )
        ),
        PresetSequence(
            name = "PROGRESS",
            description = "Linear fill left to right",
            icon = Icons.Default.LinearScale,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3), 300),
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 3), 300),
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 3, ch[2] to 3), 300),
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 3, ch[2] to 3, ch[3] to 3), 300),
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 3, ch[2] to 3, ch[3] to 3, ch[4] to 3), 300),
                GlyphSequence(ch.associateWith { 3 }, 500),
                GlyphSequence(ch.associateWith { 0 }, 200),
            )
        ),
        PresetSequence(
            name = "BOUNCE",
            description = "Ping-pong A1↔A6",
            icon = Icons.Default.SwapHoriz,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 1), 150),
                GlyphSequence(mapOf(ch[1] to 3, ch[0] to 1, ch[2] to 1), 150),
                GlyphSequence(mapOf(ch[2] to 3, ch[1] to 1, ch[3] to 1), 150),
                GlyphSequence(mapOf(ch[3] to 3, ch[2] to 1, ch[4] to 1), 150),
                GlyphSequence(mapOf(ch[4] to 3, ch[3] to 1, ch[5] to 1), 150),
                GlyphSequence(mapOf(ch[5] to 3, ch[4] to 1), 150),
                GlyphSequence(mapOf(ch[4] to 3, ch[5] to 1, ch[3] to 1), 150),
                GlyphSequence(mapOf(ch[3] to 3, ch[4] to 1, ch[2] to 1), 150),
                GlyphSequence(mapOf(ch[2] to 3, ch[3] to 1, ch[1] to 1), 150),
                GlyphSequence(mapOf(ch[1] to 3, ch[2] to 1, ch[0] to 1), 150),
            )
        ),
        PresetSequence(
            name = "CENTER",
            description = "Fills from edges to middle",
            icon = Icons.Default.Compress,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3, ch[5] to 3), 300),
                GlyphSequence(mapOf(ch[0] to 2, ch[5] to 2, ch[1] to 3, ch[4] to 3), 300),
                GlyphSequence(mapOf(ch[1] to 2, ch[4] to 2, ch[2] to 3, ch[3] to 3), 300),
                GlyphSequence(ch.associateWith { 3 }, 400),
                GlyphSequence(ch.associateWith { 0 }, 200),
            )
        )
    )
}

// ─── Screen ─────────────────────────────────────────────────────────────────
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    composerViewModel: ComposerViewModel,
    musicSyncViewModel: MusicSyncViewModel,
    modifier: Modifier = Modifier
) {
    val savedPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    val musicProjects by viewModel.allMusicProjects.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val composerState by composerViewModel.uiState.collectAsStateWithLifecycle()
    val musicSyncState by musicSyncViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Sequences", "Synced")

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp)
    ) {
        // Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "LIBRARY",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            if (composerState.isPlaying || musicSyncState.isAudioPlaying) {
                IconButton(onClick = {
                    composerViewModel.stopPlayback()
                    if (musicSyncState.isAudioPlaying) musicSyncViewModel.toggleMusicPlayback()
                }) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color(0xFFFF5252))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color(0xFF161616),
            contentColor = Color.White,
            indicator = { },
            divider = {},
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            tabs.forEachIndexed { i, label ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (selectedTab == i) Color.White else Color.Gray
                        )
                    },
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selectedTab == i) Color(0xFF333333) else Color.Transparent)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Content
        when (selectedTab) {
            0 -> SequencesTab(
                isPlaying = composerState.isPlaying,
                isPaused = composerState.isPaused,
                activePlaylistId = composerState.activePlaylistId,
                activeSequenceName = composerState.sequenceName,
                savedPlaylists = savedPlaylists,
                composerViewModel = composerViewModel,
                viewModel = viewModel
            )
            1 -> SyncedTab(
                isAudioPlaying = musicSyncState.isAudioPlaying,
                activeProjectId = musicSyncState.activeProjectId,
                projects = musicProjects,
                musicSyncViewModel = musicSyncViewModel,
                viewModel = viewModel
            )
        }
    }
}

// ─── Sequences Tab ──────────────────────────────────────────────────────────
@Composable
private fun SequencesTab(
    isPlaying: Boolean,
    isPaused: Boolean,
    activePlaylistId: Long?,
    activeSequenceName: String,
    savedPlaylists: List<PlaylistWithSteps>,
    composerViewModel: ComposerViewModel,
    viewModel: LibraryViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // My Saved Sequences
        if (savedPlaylists.isNotEmpty()) {
            item {
                Text("MY SEQUENCES", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            items(savedPlaylists) { item ->
                val active = activePlaylistId == item.playlist.id
                val playing = active && !isPaused && isPlaying
                SavedSequenceCard(
                    playlist = item,
                    isPlaying = playing,
                    isActive = active,
                    onPlay = { composerViewModel.playSequence(item) },
                    onDelete = { composerViewModel.deletePlaylist(item.playlist) }
                )
            }
        }

        // Default Presets
        item {
            Text("FACTORY PRESETS", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.heightIn(max = 1000.dp)
            ) {
                items(presetSequences) { preset ->
                    val playing = isPlaying && activeSequenceName == preset.name
                    PresetCard(
                        preset = preset,
                        isPlaying = playing,
                        onClick = { 
                            if (playing) {
                                composerViewModel.stopPlayback()
                            } else {
                                composerViewModel.startPlayback(preset.steps, null) 
                                composerViewModel.onSequenceNameChange(preset.name)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedSequenceCard(
    playlist: PlaylistWithSteps,
    isActive: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isActive) Color.White else Color(0xFF222222))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(40.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.playlist.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("${playlist.steps.size} steps", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = if (isPlaying) Color.White else Color(0xFF00C853)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun PresetCard(
    preset: PresetSequence,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (isPlaying) Color(0xFF1A1A1A) else Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isPlaying) Color.White else Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(preset.icon, contentDescription = null, tint = if (isPlaying) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isPlaying) Color.White else Color(0xFF444444),
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(preset.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(preset.description, color = Color.Gray, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─── Synced Tab ─────────────────────────────────────────────────────────────
@Composable
private fun SyncedTab(
    isAudioPlaying: Boolean,
    activeProjectId: Long?,
    projects: List<MusicProjectWithEvents>,
    musicSyncViewModel: MusicSyncViewModel,
    viewModel: LibraryViewModel
) {
    if (projects.isEmpty()) {
        EmptySyncedState()
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(projects) { project ->
                val isLoaded = activeProjectId == project.project.id
                val playing = isAudioPlaying && isLoaded
                SyncedProjectCard(
                    project = project,
                    isLoaded = isLoaded,
                    isPlaying = playing,
                    onPlay = { musicSyncViewModel.playMusicProject(project) },
                    onDelete = { musicSyncViewModel.deleteMusicProject(project.project) }
                )
            }
        }
    }
}

@Composable
private fun SyncedProjectCard(
    project: MusicProjectWithEvents,
    isLoaded: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isLoaded) Color(0xFF00C853) else Color(0xFF222222))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).background(Color(0xFF1A1A1A), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isLoaded) Icons.Default.MusicNote else Icons.Default.LibraryMusic,
                    contentDescription = null,
                    tint = if (isLoaded) Color(0xFF00C853) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(project.project.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text("${project.events.size} light events", color = Color.Gray, fontSize = 12.sp)
            }
            IconButton(onClick = onPlay) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = if (isLoaded) Color.White else Color(0xFF00C853)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252).copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun EmptySyncedState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF222222), modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("No music projects yet", color = Color.Gray, fontWeight = FontWeight.Medium)
            Text("Create one in the Music tab", color = Color(0xFF333333), fontSize = 12.sp)
        }
    }
}
