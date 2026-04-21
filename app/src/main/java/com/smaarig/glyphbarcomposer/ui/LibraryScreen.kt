package com.smaarig.glyphbarcomposer.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
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
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.LibraryViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

// ─── Constants ──────────────────────────────────────────────────────────────
private val ch = listOf(
    getChannelForIndex(0),
    getChannelForIndex(1),
    getChannelForIndex(2),
    getChannelForIndex(3),
    getChannelForIndex(4),
    getChannelForIndex(5),
    getChannelForIndex(6)
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
            description = "Slow intensity ramp",
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
            name = "SIRIUS",
            description = "Cinematic slow build",
            icon = Icons.Default.BrightnessHigh,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 1, ch[5] to 1), 800),
                GlyphSequence(mapOf(ch[0] to 2, ch[1] to 1, ch[5] to 2, ch[4] to 1), 800),
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 2, ch[2] to 1, ch[5] to 3, ch[4] to 2, ch[3] to 1), 800),
                GlyphSequence(ch.associateWith { 3 }, 1200),
                GlyphSequence(ch.associateWith { 0 }, 500),
            )
        ),
        PresetSequence(
            name = "GLITCH",
            description = "Rapid random flashes",
            icon = Icons.Default.AutoFixHigh,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3, ch[3] to 3, ch[6] to 3), 50),
                GlyphSequence(ch.associateWith { 0 }, 50),
                GlyphSequence(mapOf(ch[1] to 3, ch[4] to 3, ch[2] to 3), 50),
                GlyphSequence(ch.associateWith { 0 }, 100),
                GlyphSequence(mapOf(ch[5] to 3, ch[0] to 2, ch[6] to 3), 50),
                GlyphSequence(ch.associateWith { 0 }, 50),
            )
        ),
        PresetSequence(
            name = "HEARTBEAT",
            description = "Double pulse rhythm",
            icon = Icons.Default.Favorite,
            steps = listOf(
                GlyphSequence(mapOf(ch[2] to 3, ch[3] to 3, ch[6] to 1), 150),
                GlyphSequence(ch.associateWith { 0 }, 100),
                GlyphSequence(mapOf(ch[2] to 3, ch[3] to 3, ch[6] to 3), 250),
                GlyphSequence(ch.associateWith { 0 }, 600),
            )
        ),
        PresetSequence(
            name = "METEOR",
            description = "Sweep with long tail",
            icon = Icons.Default.Flare,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3), 100),
                GlyphSequence(mapOf(ch[0] to 2, ch[1] to 3), 100),
                GlyphSequence(mapOf(ch[0] to 1, ch[1] to 2, ch[2] to 3), 100),
                GlyphSequence(mapOf(ch[1] to 1, ch[2] to 2, ch[3] to 3), 100),
                GlyphSequence(mapOf(ch[2] to 1, ch[3] to 2, ch[4] to 3), 100),
                GlyphSequence(mapOf(ch[3] to 1, ch[4] to 2, ch[5] to 3), 100),
                GlyphSequence(mapOf(ch[4] to 1, ch[5] to 2), 100),
                GlyphSequence(mapOf(ch[5] to 1), 100),
                GlyphSequence(ch.associateWith { 0 }, 200),
            )
        ),
        PresetSequence(
            name = "CHASE",
            description = "Racing dots",
            icon = Icons.Default.DirectionsRun,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3), 80),
                GlyphSequence(mapOf(ch[1] to 3), 80),
                GlyphSequence(mapOf(ch[2] to 3), 80),
                GlyphSequence(mapOf(ch[3] to 3), 80),
                GlyphSequence(mapOf(ch[4] to 3), 80),
                GlyphSequence(mapOf(ch[5] to 3), 80),
            )
        ),
        PresetSequence(
            name = "FADE",
            description = "Cross-fading intensities",
            icon = Icons.Default.Gradient,
            steps = listOf(
                GlyphSequence(mapOf(ch[0] to 3, ch[1] to 2, ch[2] to 1), 300),
                GlyphSequence(mapOf(ch[1] to 3, ch[2] to 2, ch[3] to 1), 300),
                GlyphSequence(mapOf(ch[2] to 3, ch[3] to 2, ch[4] to 1), 300),
                GlyphSequence(mapOf(ch[3] to 3, ch[4] to 2, ch[5] to 1), 300),
                GlyphSequence(mapOf(ch[4] to 3, ch[5] to 2, ch[0] to 1), 300),
                GlyphSequence(mapOf(ch[5] to 3, ch[0] to 2, ch[1] to 1), 300),
            )
        )
    )
}

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val savedPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    val musicProjects by viewModel.allMusicProjects.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val composerState by composerViewModel.uiState.collectAsStateWithLifecycle()
    val musicStudioState by musicStudioViewModel.uiState.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("SEQUENCES", "STUDIO")

    Box(modifier = modifier.fillMaxSize().background(Color.Transparent)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "LIBRARY",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        fontFamily = com.smaarig.glyphbarcomposer.ui.theme.nothingFont
                    )
                    Text("Your creations and presets", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
                
                if (composerState.isPlaying || musicStudioState.isAudioPlaying) {
                    IconButton(
                        onClick = {
                            composerViewModel.stopPlayback()
                            if (musicStudioState.isAudioPlaying) musicStudioViewModel.toggleMusicPlayback()
                        },
                        modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
                    ) {
                        Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252))
                    }
                }
            }

            // Custom Tab Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xFF111111))
                    .padding(4.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    val bg by animateColorAsState(if (selected) Color.White else Color.Transparent)
                    val textCol by animateColorAsState(if (selected) Color.Black else Color.Gray)
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(22.dp))
                            .background(bg)
                            .clickable { selectedTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = textCol, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                    }
                }
            }

            // Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> SequencesTab(
                        isPlaying = composerState.isPlaying,
                        isPaused = composerState.isPaused,
                        activePlaylistId = composerState.activePlaylistId,
                        activeSequenceName = composerState.sequenceName,
                        savedPlaylists = savedPlaylists,
                        composerViewModel = composerViewModel
                    )
                    1 -> StudioTab(
                        isAudioPlaying = musicStudioState.isAudioPlaying,
                        activeProjectId = musicStudioState.activeProjectId,
                        projects = musicProjects,
                        musicStudioViewModel = musicStudioViewModel
                    )
                }
            }
            
            Spacer(Modifier.height(120.dp))
        }
    }
}

@Composable
private fun SequencesTab(
    isPlaying: Boolean,
    isPaused: Boolean,
    activePlaylistId: Long?,
    activeSequenceName: String,
    savedPlaylists: List<PlaylistWithSteps>,
    composerViewModel: ComposerViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (savedPlaylists.isNotEmpty()) {
            item { Text("SAVED SEQUENCES", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
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

        item { Text("FACTORY PRESETS", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.heightIn(max = 2000.dp)
            ) {
                items(presetSequences) { preset ->
                    val playing = isPlaying && activeSequenceName == preset.name
                    PresetCard(
                        preset = preset,
                        isPlaying = playing,
                        onClick = { 
                            if (playing) composerViewModel.stopPlayback()
                            else { composerViewModel.startPlayback(preset.steps, null); composerViewModel.onSequenceNameChange(preset.name) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StudioTab(
    isAudioPlaying: Boolean,
    activeProjectId: Long?,
    projects: List<MusicProjectWithEvents>,
    musicStudioViewModel: MusicStudioViewModel
) {
    if (projects.isEmpty()) {
        EmptyStateView(Icons.Default.MusicNote, "No studio projects", "Create one in the Music Studio tab")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { Text("RECENT PROJECTS", color = Color(0xFF555555), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
            items(projects) { project ->
                val isLoaded = activeProjectId == project.project.id
                val playing = isAudioPlaying && isLoaded
                StudioProjectCard(
                    project = project,
                    isLoaded = isLoaded,
                    isPlaying = playing,
                    onPlay = { musicStudioViewModel.playMusicProject(project) },
                    onDelete = { musicStudioViewModel.deleteMusicProject(project.project) }
                )
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
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) Color.White else Color(0xFF222222))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFF1A1A1A), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Tune, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(playlist.playlist.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("${playlist.steps.size} steps", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onPlay, modifier = Modifier.clip(CircleShape).background(if (isPlaying) Color.White else Color(0xFF1A1A1A))) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = if (isPlaying) Color.Black else Color(0xFF00C853))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252).copy(0.4f), modifier = Modifier.size(18.dp))
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
        modifier = Modifier.fillMaxWidth().height(130.dp).clip(RoundedCornerShape(24.dp)).clickable { onClick() },
        color = if (isPlaying) Color(0xFF1A1A1A) else Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isPlaying) Color.White else Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(preset.icon, null, tint = if (isPlaying) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                if (isPlaying) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
            }
            Column {
                Text(preset.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp)
                Text(preset.description, color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun StudioProjectCard(
    project: MusicProjectWithEvents,
    isLoaded: Boolean,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isLoaded) Color(0xFF00C853) else Color(0xFF222222))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Color(0xFF1A1A1A), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.GraphicEq, null, tint = if (isLoaded) Color(0xFF00C853) else Color.Gray, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(project.project.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
                Text("${project.events.size} light beats", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onPlay, modifier = Modifier.clip(CircleShape).background(if (isPlaying) Color.White else Color(0xFF1A1A1A))) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = if (isPlaying) Color.Black else Color(0xFF00C853))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF5252).copy(0.4f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun EmptyStateView(icon: ImageVector, title: String, sub: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFF1A1A1A), modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 14.sp)
            Text(sub, color = Color(0xFF333333), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
