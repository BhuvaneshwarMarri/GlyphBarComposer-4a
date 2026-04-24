package com.smaarig.glyphbarcomposer.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.io.File
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.MusicStudioProject
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.LibraryViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState

private val ch = listOf(
    com.nothing.ketchum.Glyph.Code_25111.A_1,
    com.nothing.ketchum.Glyph.Code_25111.A_2,
    com.nothing.ketchum.Glyph.Code_25111.A_3,
    com.nothing.ketchum.Glyph.Code_25111.A_4,
    com.nothing.ketchum.Glyph.Code_25111.A_5,
    com.nothing.ketchum.Glyph.Code_25111.A_6,
    com.nothing.ketchum.Glyph.Code_22111.E1
)

data class PresetSequence(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val steps: List<GlyphSequence>
)

val presetSequences = listOf(
    PresetSequence("Pulse", "Steady rhythmic breathing", Icons.Default.Favorite, List(4) { i ->
        GlyphSequence(ch.associateWith { if (i % 2 == 0) 3 else 0 }, 500)
    }),
    PresetSequence("Wave", "Smooth horizontal sweep", Icons.Default.Waves, List(12) { i ->
        val active = if (i < 6) i else 11 - i
        GlyphSequence(mapOf(ch[active.coerceIn(0, 6)] to 3), 80)
    }),
    PresetSequence("Strobe", "High intensity flashing", Icons.Default.FlashOn, List(2) { i ->
        GlyphSequence(ch.associateWith { if (i == 0) 3 else 0 }, 100)
    }),
    PresetSequence("Knight Rider", "Back and forth pulse", Icons.AutoMirrored.Filled.DirectionsRun, List(10) { i ->
        val active = if (i < 6) i else 10 - i
        GlyphSequence(mapOf(ch[active.coerceIn(0, 5)] to 3), 100)
    }),
    PresetSequence("Fire", "Warm flickering glow", Icons.Default.Whatshot, List(8) {
        val intensities = ch.associateWith { (1..3).random() }
        GlyphSequence(intensities, (80..150).random())
    }),
    PresetSequence("Police", "Emergency response signal", Icons.Default.Warning, List(4) { i ->
        val map = if (i < 2) mapOf(ch[0] to 3, ch[1] to 3, ch[2] to 3) else mapOf(ch[3] to 3, ch[4] to 3, ch[5] to 3)
        GlyphSequence(map, 150)
    }),
    PresetSequence("Heartbeat", "Double rhythmic thump", Icons.Default.MonitorHeart, listOf(
        GlyphSequence(ch.associateWith { 3 }, 150),
        GlyphSequence(ch.associateWith { 0 }, 100),
        GlyphSequence(ch.associateWith { 2 }, 150),
        GlyphSequence(ch.associateWith { 0 }, 600)
    )),
    PresetSequence("Matrix", "Digital rain descent", Icons.Default.Code, List(14) { i ->
        val active = i % 7
        GlyphSequence(mapOf(ch[active] to 3), 120)
    })
)

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle(emptyList())
    val studioProjects by viewModel.allMusicProjects.collectAsStateWithLifecycle(emptyList())
    val compState by composerViewModel.uiState.collectAsStateWithLifecycle()
    val studioState by musicStudioViewModel.uiState.collectAsStateWithLifecycle()
    val orientation = rememberAppOrientation()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    if (orientation == AppOrientation.Landscape) {
        LibraryLandscape(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            playlists = playlists,
            studioProjects = studioProjects,
            compState = compState,
            studioState = studioState,
            composerViewModel = composerViewModel,
            musicStudioViewModel = musicStudioViewModel,
            onSharePlaylist = { viewModel.exportPlaylist(context, it) },
            onShareStudio = { viewModel.exportMusicProject(context, it) }
        )
    } else {
        LibraryPortrait(
            selectedTab = selectedTab,
            onTabSelect = { selectedTab = it },
            playlists = playlists,
            studioProjects = studioProjects,
            compState = compState,
            studioState = studioState,
            composerViewModel = composerViewModel,
            musicStudioViewModel = musicStudioViewModel,
            onSharePlaylist = { viewModel.exportPlaylist(context, it) },
            onShareStudio = { viewModel.exportMusicProject(context, it) }
        )
    }
}

@Composable
fun LibraryPortrait(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    playlists: List<PlaylistWithSteps>,
    studioProjects: List<MusicProjectWithEvents>,
    compState: com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState,
    studioState: com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    onSharePlaylist: (PlaylistWithSteps) -> Unit,
    onShareStudio: (MusicProjectWithEvents) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
        LibraryHeader(
            onStopAll = {
                composerViewModel.stopPlayback()
                if (studioState.isAudioPlaying) musicStudioViewModel.toggleMusicPlayback()
            }, 
            isAnyPlaying = compState.isPlaying || studioState.isAudioPlaying
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color(0xFF111111))
                .padding(4.dp)
        ) {
            listOf("SEQUENCES", "MUSIC STUDIO").forEachIndexed { index, label ->
                val selected = selectedTab == index
                val bg by animateColorAsState(if (selected) Color.White else Color.Transparent)
                val textCol by animateColorAsState(if (selected) Color.Black else Color.Gray)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(22.dp))
                        .background(bg)
                        .clickable { onTabSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = textCol, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            if (selectedTab == 0) {
                SequencesTab(compState.isPlaying, compState.isPaused, compState.activePlaylistId, playlists, composerViewModel, onSharePlaylist)
            } else {
                StudioTab(studioState.isAudioPlaying, studioState.activeProjectId, studioProjects, musicStudioViewModel, onShareStudio)
            }
        }
    }
}

@Composable
fun LibraryLandscape(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    playlists: List<PlaylistWithSteps>,
    studioProjects: List<MusicProjectWithEvents>,
    compState: com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState,
    studioState: com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    onSharePlaylist: (PlaylistWithSteps) -> Unit,
    onShareStudio: (MusicProjectWithEvents) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Column(modifier = Modifier.width(220.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            LibraryHeader(
                onStopAll = {
                    composerViewModel.stopPlayback()
                    if (studioState.isAudioPlaying) musicStudioViewModel.toggleMusicPlayback()
                }, 
                isAnyPlaying = compState.isPlaying || studioState.isAudioPlaying
            )
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LibNavItem("SEQUENCES", Icons.AutoMirrored.Filled.List, selectedTab == 0) { onTabSelect(0) }
                LibNavItem("MUSIC STUDIO", Icons.Default.GraphicEq, selectedTab == 1) { onTabSelect(1) }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            if (selectedTab == 0) {
                SequencesTab(compState.isPlaying, compState.isPaused, compState.activePlaylistId, playlists, composerViewModel, onSharePlaylist)
            } else {
                StudioTab(studioState.isAudioPlaying, studioState.activeProjectId, studioProjects, musicStudioViewModel, onShareStudio)
            }
        }
    }
}

@Composable
private fun LibNavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable { onClick() },
        color = if (selected) Color.White else Color.Transparent,
        shape = RoundedCornerShape(16.dp),
        border = if (!selected) BorderStroke(1.dp, Color(0xFF222222)) else null
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = if (selected) Color.Black else Color.Gray, modifier = Modifier.size(20.dp))
            Text(label, color = if (selected) Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun LibraryHeader(onStopAll: () -> Unit, isAnyPlaying: Boolean) {
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
        }

        if (isAnyPlaying) {
            IconButton(
                onClick = onStopAll,
                modifier = Modifier.clip(CircleShape).background(Color(0x1AFF5252))
            ) {
                Icon(Icons.Default.Stop, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun SequencesTab(isPlaying: Boolean, isPaused: Boolean, activeId: Long?, playlists: List<PlaylistWithSteps>, viewModel: ComposerViewModel, onShare: (PlaylistWithSteps) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionLabel("GLYPH  PRESETS") }
        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                presetSequences.forEach { preset ->
                    PresetCard(preset, false) { viewModel.startPlayback(preset.steps, null) }
                }
            }
        }
        
        item { SectionLabel("MY SEQUENCES") }
        if (playlists.isEmpty()) {
            item { EmptyStateView(Icons.Default.Create, "No Sequences", "Create one in the Composer tab") }
        } else {
            items(playlists, key = { it.playlist.id }) { playlist ->
                SavedSequenceCard(
                    playlist = playlist,
                    isActive = activeId == playlist.playlist.id,
                    isPlaying = isPlaying,
                    isPaused = isPaused,
                    onPlay = { viewModel.playSequence(playlist) }, 
                    onDelete = { viewModel.deletePlaylist(playlist.playlist) },
                    onShare = { onShare(playlist) }
                )
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun StudioTab(
    isPlaying: Boolean,
    activeId: Long?,
    projects: List<MusicProjectWithEvents>,
    viewModel: MusicStudioViewModel,
    onShare: (MusicProjectWithEvents) -> Unit
) {
    var projectToRelink by remember { mutableStateOf<MusicProjectWithEvents?>(null) }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { selectedUri ->
            projectToRelink?.let { project ->
                viewModel.relinkAudioAndPlay(project, selectedUri)
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (projects.isEmpty()) {
            item { EmptyStateView(Icons.Default.Audiotrack, "No Projects", "Sync a track in the Music Studio") }
        } else {
            items(projects, key = { it.project.id }) { project ->
                val isAudioMissing = project.project.localAudioPath.isBlank() || !File(project.project.localAudioPath).exists()
                
                StudioProjectCard(
                    project = project,
                    isActive = activeId == project.project.id,
                    isPlaying = isPlaying && activeId == project.project.id,
                    isAudioMissing = isAudioMissing,
                    onPlay = {
                        if (isAudioMissing) {
                            projectToRelink = project
                            audioPicker.launch("audio/*")
                        } else {
                            if (activeId == project.project.id) {
                                viewModel.toggleMusicPlayback()
                            } else {
                                viewModel.playMusicProject(project)
                            }
                        }
                    },
                    onDelete = { viewModel.deleteMusicProject(project.project) },
                    onShare = { onShare(project) }
                )
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}

@Composable
private fun SavedSequenceCard(
    playlist: PlaylistWithSteps,
    isActive: Boolean,
    isPlaying: Boolean,
    isPaused: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPlay() },
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00C853) else Color(0xFF222222))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isActive) Color(0xFF00C853) else Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    !isActive -> Icons.AutoMirrored.Filled.PlaylistPlay
                    isPlaying && !isPaused -> Icons.Default.Pause
                    else -> Icons.Default.PlayArrow
                }
                Icon(icon, null, tint = if (isActive) Color.Black else Color.Gray)
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(playlist.playlist.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("${playlist.steps.size} steps", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(0.4f))
            }
        }
    }
}

@Composable
private fun PresetCard(preset: PresetSequence, isActive: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.size(width = 140.dp, height = 140.dp).clickable { onClick() },
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00C853) else Color(0xFF222222))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(preset.icon, null, tint = if (isActive) Color(0xFF00C853) else Color.Gray, modifier = Modifier.size(28.dp))
            Column {
                Text(preset.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                Text(preset.description, color = Color.Gray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StudioProjectCard(
    project: MusicProjectWithEvents,
    isActive: Boolean,
    isPlaying: Boolean,
    isAudioMissing: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPlay() },
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isActive) Color(0xFF00C853) else if (isAudioMissing) Color(0xFFFF5252).copy(alpha = 0.5f) else Color(0xFF222222))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive) Color(0xFF00C853) 
                        else if (isAudioMissing) Color(0xFFFF5252).copy(alpha = 0.1f)
                        else Color(0xFF1A1A1A)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isActive && isPlaying -> Icons.Default.Pause
                        isAudioMissing -> Icons.Default.MusicOff
                        else -> Icons.Default.PlayArrow
                    },
                    contentDescription = null,
                    tint = if (isActive) Color.Black else if (isAudioMissing) Color(0xFFFF5252) else Color.Gray
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(project.project.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${project.events.size} sync events", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (isAudioMissing) {
                        Spacer(Modifier.width(8.dp))
                        Text("• MISSING AUDIO", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            IconButton(onClick = onShare) {
                Icon(Icons.Default.Share, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.Gray.copy(0.4f))
            }
        }
    }
}

@Composable
private fun EmptyStateView(icon: ImageVector, title: String, description: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(icon, null, tint = Color(0xFF1A1A1A), modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(description, color = Color(0xFF333333), fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}
