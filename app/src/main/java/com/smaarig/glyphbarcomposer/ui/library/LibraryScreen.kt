package com.smaarig.glyphbarcomposer.ui.library

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.AppOrientation
import com.smaarig.glyphbarcomposer.ui.Screen
import com.smaarig.glyphbarcomposer.ui.library.components.*
import com.smaarig.glyphbarcomposer.ui.rememberAppOrientation
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.ComposerViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.LibraryViewModel
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioUiState
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle(emptyList())
    val studioProjects by viewModel.allMusicProjects.collectAsStateWithLifecycle(emptyList())
    val compState by composerViewModel.uiState.collectAsStateWithLifecycle()
    val studioState by musicStudioViewModel.uiState.collectAsStateWithLifecycle()
    val isExporting by viewModel.isExporting.collectAsStateWithLifecycle()
    val exportError by viewModel.exportError.collectAsStateWithLifecycle()

    val orientation = rememberAppOrientation()
    val context = LocalContext.current

    var selectedTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(exportError) {
        exportError?.let {
            // Show error message if needed
            viewModel.clearExportError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                onEditSequence = {
                    composerViewModel.editPlaylist(it)
                    navController.navigate(Screen.Composer.route)
                },
                onEditStudio = {
                    musicStudioViewModel.editMusicProject(it)
                    navController.navigate(Screen.MusicStudio.route)
                },
                onExportOgg = { viewModel.exportAsOgg(context, it) },
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
                onEditSequence = {
                    composerViewModel.editPlaylist(it)
                    navController.navigate(Screen.Composer.route)
                },
                onEditStudio = {
                    musicStudioViewModel.editMusicProject(it)
                    navController.navigate(Screen.MusicStudio.route)
                },
                onExportOgg = { viewModel.exportAsOgg(context, it) },
                onSharePlaylist = { viewModel.exportPlaylist(context, it) },
                onShareStudio = { viewModel.exportMusicProject(context, it) }
            )
        }

        if (isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color(0xFF111111),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF222222))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF00C853))
                        Spacer(Modifier.height(16.dp))
                        Text("Exporting OGG...", color = Color.White, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryPortrait(
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    playlists: List<PlaylistWithSteps>,
    studioProjects: List<MusicProjectWithEvents>,
    compState: ComposerUiState,
    studioState: MusicStudioUiState,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    onEditSequence: (PlaylistWithSteps) -> Unit,
    onEditStudio: (MusicProjectWithEvents) -> Unit,
    onExportOgg: (MusicProjectWithEvents) -> Unit,
    onSharePlaylist: (PlaylistWithSteps) -> Unit,
    onShareStudio: (MusicProjectWithEvents) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
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
                val bg by animateColorAsState(
                    if (selected) {
                        if (index == 0) Color(0xFF0086EA) else Color(0xFFFFC1CC)
                    } else Color.Transparent,
                    label = "tabBg"
                )
                val textCol by animateColorAsState(if (selected) Color.Black else Color.Gray, label = "tabText")

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
                SequencesTab(compState.isPlaying, compState.isPaused, compState.activePlaylistId, playlists, composerViewModel, onEditSequence, onSharePlaylist)
            } else {
                StudioTab(studioState.isAudioPlaying, studioState.activeProjectId, studioProjects, musicStudioViewModel, onEditStudio, onExportOgg, onShareStudio)
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
    compState: ComposerUiState,
    studioState: MusicStudioUiState,
    composerViewModel: ComposerViewModel,
    musicStudioViewModel: MusicStudioViewModel,
    onEditSequence: (PlaylistWithSteps) -> Unit,
    onEditStudio: (MusicProjectWithEvents) -> Unit,
    onExportOgg: (MusicProjectWithEvents) -> Unit,
    onSharePlaylist: (PlaylistWithSteps) -> Unit,
    onShareStudio: (MusicProjectWithEvents) -> Unit
) {
    Row(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
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
                SequencesTab(compState.isPlaying, compState.isPaused, compState.activePlaylistId, playlists, composerViewModel, onEditSequence, onSharePlaylist)
            } else {
                StudioTab(studioState.isAudioPlaying, studioState.activeProjectId, studioProjects, musicStudioViewModel, onEditStudio, onExportOgg, onShareStudio)
            }
        }
    }
}

