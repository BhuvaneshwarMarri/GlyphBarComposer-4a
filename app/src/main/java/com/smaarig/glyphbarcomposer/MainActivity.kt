package com.smaarig.glyphbarcomposer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nothing.ketchum.Glyph
import com.smaarig.glyphbarcomposer.ui.theme.GlyphBarComposerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var glyphController: GlyphController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        glyphController = GlyphController()
        glyphController.init(this)

        enableEdgeToEdge()
        setContent {
            GlyphBarComposerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF121212)
                ) { innerPadding ->
                    GlyphSequencerScreen(
                        modifier = Modifier.padding(innerPadding),
                        glyphController = glyphController
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        glyphController.deinit()
    }
}

@Composable
fun GlyphSequencerScreen(
    modifier: Modifier = Modifier,
    glyphController: GlyphController,
    viewModel: GlyphViewModel = viewModel()
) {
    val glyphStates = remember { mutableStateListOf(false, false, false, false, false, false) }
    var durationMs by remember { mutableStateOf(1000f) }
    val currentSequenceSteps = remember { mutableStateListOf<GlyphSequence>() }
    var sequenceName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }

    val savedSequences by viewModel.allPlaylists.collectAsState(initial = emptyList())

    val channels = listOf(
        Glyph.Code_25111.A_1,
        Glyph.Code_25111.A_2,
        Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4,
        Glyph.Code_25111.A_5,
        Glyph.Code_25111.A_6
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Glyph Composer for 4a",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        // LED Selection Strip
        Text(
            text = "Step 1: Select LEDs",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            glyphStates.forEachIndexed { index, isActive ->
                GlyphBox(
                    label = "A${index + 1}",
                    isActive = isActive,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        glyphStates[index] = !isActive
                        val active = mutableListOf<Int>()
                        glyphStates.forEachIndexed { i, a -> if (a) active.add(channels[i]) }
                        glyphController.applyGlyphState(active, 2000)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Duration Slider
        Text(
            text = "Step 2: Set Duration (${durationMs.toInt()}ms)",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Slider(
            value = durationMs,
            onValueChange = { durationMs = it },
            valueRange = 100f..5000f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
        )

        // Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val active = mutableListOf<Int>()
                    glyphStates.forEachIndexed { i, a -> if (a) active.add(channels[i]) }
                    currentSequenceSteps.add(GlyphSequence(active, durationMs.toInt()))
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("+ Add to Sequence")
            }

            Button(
                onClick = {
                    if (!isPlaying && currentSequenceSteps.isNotEmpty()) {
                        isPlaying = true
                        coroutineScope.launch {
                            currentSequenceSteps.forEach { step ->
                                // Sync UI boxes
                                channels.forEachIndexed { i, ch ->
                                    glyphStates[i] = step.activeChannels.contains(ch)
                                }
                                glyphController.playSingleFrame(step.activeChannels, step.durationMs)
                                delay(step.durationMs.toLong() + 50)
                            }
                            channels.forEachIndexed { i, _ -> glyphStates[i] = false }
                            glyphController.turnOffGlyphs()
                            isPlaying = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                enabled = !isPlaying && currentSequenceSteps.isNotEmpty(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isPlaying) "Playing..." else "Play Current")
            }
        }

        // Current Sequence Steps Preview
        if (currentSequenceSteps.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Current Sequence Steps", color = Color.Gray, fontSize = 14.sp)
                Text(
                    "Clear",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { currentSequenceSteps.clear() }
                )
            }
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(currentSequenceSteps) { step ->
                    StepPreviewBox(step)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Save Sequence
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = sequenceName,
                onValueChange = { sequenceName = it },
                label = { Text("Sequence Name", color = Color.Gray) },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray
                )
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    if (sequenceName.isNotBlank() && currentSequenceSteps.isNotEmpty()) {
                        viewModel.savePlaylist(sequenceName, currentSequenceSteps.toList())
                        sequenceName = ""
                        currentSequenceSteps.clear()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Save")
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color.DarkGray)
        Spacer(Modifier.height(16.dp))

        // Sequence List (Saved)
        Text("Sequence List", color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(savedSequences) { playlistWithSteps ->
                SavedSequenceRow(
                    playlistWithSteps = playlistWithSteps,
                    onPlay = {
                        if (!isPlaying) {
                            isPlaying = true
                            coroutineScope.launch {
                                playlistWithSteps.steps.sortedBy { it.stepIndex }.forEach { step ->
                                    // Sync UI boxes
                                    channels.forEachIndexed { i, ch ->
                                        glyphStates[i] = step.activeChannels.contains(ch)
                                    }
                                    glyphController.playSingleFrame(step.activeChannels, step.durationMs)
                                    delay(step.durationMs.toLong() + 50)
                                }
                                channels.forEachIndexed { i, _ -> glyphStates[i] = false }
                                glyphController.turnOffGlyphs()
                                isPlaying = false
                            }
                        }
                    },
                    onDelete = { viewModel.deletePlaylist(playlistWithSteps.playlist) }
                )
            }
        }
    }
}

@Composable
fun StepPreviewBox(step: GlyphSequence) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
            .background(Color(0xFF252525)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${step.durationMs}ms", fontSize = 8.sp, color = Color.LightGray)
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                repeat(6) { i ->
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .background(if (step.activeChannels.contains(getChannelForIndex(i))) Color.White else Color.Black)
                    )
                }
            }
        }
    }
}

@Composable
fun SavedSequenceRow(
    playlistWithSteps: PlaylistWithSteps,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0xFF1E1E1E),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(playlistWithSteps.playlist.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${playlistWithSteps.steps.size} steps", color = Color.Gray, fontSize = 12.sp)
                }
                IconButton(onClick = onPlay) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFF00C853))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                }
            }
            
            // Steps Mini-Preview List
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(playlistWithSteps.steps.sortedBy { it.stepIndex }) { step ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(0.5.dp, Color.DarkGray, RoundedCornerShape(2.dp))
                            .background(Color(0xFF252525)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(6) { i ->
                                Box(
                                    modifier = Modifier
                                        .size(2.dp)
                                        .background(if (step.activeChannels.contains(getChannelForIndex(i))) Color.White else Color.Black)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getChannelForIndex(index: Int): Int {
    return when(index) {
        0 -> Glyph.Code_25111.A_1
        1 -> Glyph.Code_25111.A_2
        2 -> Glyph.Code_25111.A_3
        3 -> Glyph.Code_25111.A_4
        4 -> Glyph.Code_25111.A_5
        5 -> Glyph.Code_25111.A_6
        else -> 0
    }
}

@Composable
fun GlyphBox(
    label: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(if (isActive) 1.1f else 1.0f)
    val color by animateColorAsState(if (isActive) Color.White else Color.Transparent)

    Surface(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = color
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.Black else Color.White
            )
        }
    }
}
