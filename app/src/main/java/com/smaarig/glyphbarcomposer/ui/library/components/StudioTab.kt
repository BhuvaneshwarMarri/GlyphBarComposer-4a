package com.smaarig.glyphbarcomposer.ui.library.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    onEdit: (MusicProjectWithEvents) -> Unit,
    onShare: (MusicProjectWithEvents) -> Unit
) {
    var projectToRelink by remember { mutableStateOf<MusicProjectWithEvents?>(null) }

    val audioPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { selectedUri ->
                projectToRelink?.let { project ->
                    viewModel.relinkAudioAndPlay(project, selectedUri)
                }
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (projects.isEmpty()) {
            item {
                EmptyStateView(
                    Icons.Default.Audiotrack,
                    "No Projects",
                    "Sync a track in the Music Studio"
                )
            }
        } else {
            items(projects, key = { it.project.id }) { project ->
                val isAudioMissing =
                    project.project.localAudioPath.isBlank() || !File(project.project.localAudioPath).exists()

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
                    onEdit = { onEdit(project) },
                    onShare = { onShare(project) }
                )
            }
        }
        item { Spacer(Modifier.height(120.dp)) }
    }
}
