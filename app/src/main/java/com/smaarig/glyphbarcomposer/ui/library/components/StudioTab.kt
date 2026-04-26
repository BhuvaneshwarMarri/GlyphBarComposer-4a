package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smaarig.glyphbarcomposer.data.MusicProjectWithEvents
import com.smaarig.glyphbarcomposer.ui.viewmodel.MusicStudioViewModel
import java.io.File

@Composable
fun StudioTab(
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
