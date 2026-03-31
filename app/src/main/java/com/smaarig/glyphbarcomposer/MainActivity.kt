package com.smaarig.glyphbarcomposer

import android.os.Bundle
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.tooling.preview.Preview

// ─── Intensity palette ──────────────────────────────────────────────────────
// Four visually distinct, opaque levels so there is no guessing what state a
// box is in. Using opaque colours (not alpha) avoids blend-with-background
// ambiguity.
//
//  0 = OFF   → near-black
//  1 = LOW   → muted warm-white  (≈ 25 % perceived brightness)
//  2 = MED   → bright warm-white (≈ 55 % perceived brightness)
//  3 = HIGH  → pure white        (100 % perceived brightness)
//
private val intensityColor = listOf(
    Color(0xFF1C1C1C),   // 0 – OFF
    Color(0xFF686868),   // 1 – LOW   (distinct mid-grey)
    Color(0xFFCDCDCD),   // 2 – MED   (light grey-white)
    Color(0xFFFFFFFF),   // 3 – HIGH  (pure white)
)

// Border colours give an extra hint even before the fill is registered
private val intensityBorder = listOf(
    Color(0xFF3A3A3A),   // 0
    Color(0xFF888888),   // 1
    Color(0xFFE0E0E0),   // 2
    Color(0xFFFFFFFF),   // 3
)

// Label / status text that sits on top of the fill
private fun labelColor(intensity: Int) =
    if (intensity >= 2) Color(0xFF111111) else Color(0xFFFFFFFF)

private val statusLabel = listOf("", "LOW", "MED", "HIGH")

// ─── Activity ───────────────────────────────────────────────────────────────

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
                    containerColor = Color(0xFF0E0E0E)
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

// ─── Screen ─────────────────────────────────────────────────────────────────

@Composable
fun GlyphSequencerScreen(
    modifier: Modifier = Modifier,
    glyphController: GlyphController,
    viewModel: GlyphViewModel = viewModel()
) {
    val glyphIntensities = remember { mutableStateListOf(0, 0, 0, 0, 0, 0) }
    var durationMs by remember { mutableFloatStateOf(1000f) }
    val currentSequenceSteps = remember { mutableStateListOf<GlyphSequence>() }
    var sequenceName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    val savedSequences by viewModel.allPlaylists.collectAsState(initial = emptyList())

    val channels = listOf(
        Glyph.Code_25111.A_1,
        Glyph.Code_25111.A_2,
        Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4,
        Glyph.Code_25111.A_5,
        Glyph.Code_25111.A_6
    )

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        isPlaying = false
        isPaused = false
        glyphController.turnOffGlyphs()
        channels.forEachIndexed { i, _ -> glyphIntensities[i] = 0 }
    }

    fun startPlayback(steps: List<GlyphSequence>) {
        if (steps.isEmpty()) return
        stopPlayback()
        isPlaying = true
        playbackJob = coroutineScope.launch {
            try {
                while (true) {
                    for (step in steps) {
                        while (isPaused) {
                            delay(100)
                        }
                        // Sync UI boxes
                        channels.forEachIndexed { i, ch ->
                            glyphIntensities[i] = step.channelIntensities[ch] ?: 0
                        }
                        glyphController.applyGlyphStateWithIntensities(
                            step.channelIntensities, step.durationMs
                        )
                        delay(step.durationMs.toLong() + 50)
                    }
                }
            } finally {
                // Cleanup handled in stopPlayback or if cancelled
            }
        }
    }

    fun getIntensitiesMap(): Map<Int, Int> =
        channels.mapIndexed { index, ch -> ch to glyphIntensities[index] }.toMap()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Glyph Composer · 4a",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            IconButton(
                onClick = { stopPlayback() },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = Color(0xFFFF5252)
                )
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Turn off all lights",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Intensity legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("OFF", "LOW", "MED", "HIGH").forEachIndexed { i, lbl ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(intensityColor[i], RoundedCornerShape(2.dp))
                            .border(1.dp, intensityBorder[i], RoundedCornerShape(2.dp))
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(lbl, color = Color(0xFF888888), fontSize = 10.sp)
                }
                if (i < 3) Spacer(Modifier.width(4.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Step 1: LED Selection ──
        SectionLabel("Step 1 — Select LEDs  (drag ↑↓ to change level, tap to toggle)")

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            glyphIntensities.forEachIndexed { index, intensity ->
                GlyphBox(
                    label = "A${index + 1}",
                    intensity = intensity,
                    modifier = Modifier.weight(1f),
                    onIntensityChange = { newIntensity ->
                        glyphIntensities[index] = newIntensity
                        glyphController.applyGlyphStateWithIntensities(getIntensitiesMap(), 2000)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Step 2: Duration ──
        SectionLabel("Step 2 — Duration: ${durationMs.toInt()} ms")
        Slider(
            value = durationMs,
            onValueChange = { durationMs = it },
            valueRange = 250f..5000f,
            steps = 18,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF3A3A3A)
            )
        )

        // ── Action row ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    currentSequenceSteps.add(GlyphSequence(getIntensitiesMap(), durationMs.toInt()))
                },
                modifier = Modifier.weight(1f),
                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                enabled = !isPlaying
            ) {
                Text("+ Step")
            }

            if (!isPlaying) {
                Button(
                    onClick = { startPlayback(currentSequenceSteps.toList()) },
                    modifier = Modifier.weight(1.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00C853),
                        contentColor = Color.Black
                    ),
                    enabled = currentSequenceSteps.isNotEmpty(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Loop")
                }
            } else {
                Button(
                    onClick = { isPaused = !isPaused },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (isPaused) "Res" else "Pause")
                }

                Button(
                    onClick = { stopPlayback() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5252),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }
        }

        // ── Current sequence steps preview ──
        if (currentSequenceSteps.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Current Sequence  (${currentSequenceSteps.size} steps)",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
                Text(
                    "Clear",
                    color = if (isPlaying) Color(0xFF444444) else Color(0xFFFF5252),
                    fontSize = 12.sp,
                    modifier = Modifier.clickable(enabled = !isPlaying) { currentSequenceSteps.clear() }
                )
            }
            Spacer(Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(currentSequenceSteps) { step ->
                    StepPreviewBox(step)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Save ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = sequenceName,
                onValueChange = { sequenceName = it },
                label = { Text("Sequence name", color = Color(0xFF888888), fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF444444)
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp),
                enabled = !isPlaying
            ) {
                Text("Save")
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF2A2A2A))
        Spacer(Modifier.height(10.dp))

        Text(
            "Saved Sequences",
            color = Color(0xFF888888),
            style = MaterialTheme.typography.labelLarge
        )
        Spacer(Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(savedSequences) { playlistWithSteps ->
                SavedSequenceRow(
                    playlistWithSteps = playlistWithSteps,
                    onPlay = {
                        startPlayback(playlistWithSteps.steps.sortedBy { it.stepIndex }.map { step ->
                            GlyphSequence(step.channelIntensities, step.durationMs)
                        })
                    },
                    onDelete = { viewModel.deletePlaylist(playlistWithSteps.playlist) }
                )
            }
        }
    }
}

// ─── GlyphBox ────────────────────────────────────────────────────────────────
//
// Drag UP → increase intensity (capped at 3)
// Drag DOWN → decrease intensity (floored at 0)
// Tap → toggle between OFF and HIGH (3)
//
// Key change: opaque fill colours instead of alpha-blended whites.
// Spring animation gives a satisfying snap to each level change.

@Composable
fun GlyphBox(
    label: String,
    intensity: Int,
    modifier: Modifier = Modifier,
    onIntensityChange: (Int) -> Unit
) {
    var accumulatedDrag by remember { mutableStateOf(0f) }

    // Smooth spring scale: active boxes pop slightly
    val scale by animateFloatAsState(
        targetValue = if (intensity > 0) 1.08f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "scale"
    )

    // Opaque fill — each level is clearly distinct
    val fillColor by animateColorAsState(
        targetValue = intensityColor[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "fill"
    )
    val borderColor by animateColorAsState(
        targetValue = intensityBorder[intensity],
        animationSpec = tween(durationMillis = 120),
        label = "border"
    )

    // Drag feedback overlay (direction arrow hint)
    val dragProgress = (kotlin.math.abs(accumulatedDrag).coerceAtMost(30f) / 30f)
    val dragOverlayAlpha = dragProgress * 0.18f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
            .background(fillColor)
            .pointerInput(intensity) {
                detectVerticalDragGestures(
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        // 28 px threshold — responsive but not twitchy
                        val threshold = 28f
                        if (accumulatedDrag > threshold) {
                            if (intensity > 0) onIntensityChange(intensity - 1)
                            accumulatedDrag = 0f
                        } else if (accumulatedDrag < -threshold) {
                            if (intensity < 3) onIntensityChange(intensity + 1)
                            accumulatedDrag = 0f
                        }
                    },
                    onDragEnd = { accumulatedDrag = 0f },
                    onDragCancel = { accumulatedDrag = 0f }
                )
            }
            .clickable {
                if (intensity > 0) onIntensityChange(0) else onIntensityChange(3)
            },
        contentAlignment = Alignment.Center
    ) {
        // Directional drag overlay hint
        if (dragOverlayAlpha > 0f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        if (accumulatedDrag < 0) Color.White.copy(alpha = dragOverlayAlpha)
                        else Color.Black.copy(alpha = dragOverlayAlpha)
                    )
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = labelColor(intensity)
            )
            if (intensity > 0) {
                Text(
                    text = statusLabel[intensity],
                    fontSize = 8.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = labelColor(intensity).copy(alpha = 0.75f),
                    letterSpacing = 0.8.sp
                )
            }
        }
    }
}

// ─── Step preview ────────────────────────────────────────────────────────────

@Composable
fun StepPreviewBox(step: GlyphSequence) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Color(0xFF3A3A3A), RoundedCornerShape(6.dp))
            .background(Color(0xFF1A1A1A)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${step.durationMs}ms", fontSize = 8.sp, color = Color(0xFF777777))
            Spacer(Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(6) { i ->
                    val intensity = step.channelIntensities[getChannelForIndex(i)] ?: 0
                    Box(
                        modifier = Modifier
                            .size(width = 5.dp, height = 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(intensityColor[intensity])
                    )
                }
            }
        }
    }
}

// ─── Saved sequence row ──────────────────────────────────────────────────────

@Composable
fun SavedSequenceRow(
    playlistWithSteps: PlaylistWithSteps,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0xFF1A1A1A),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        playlistWithSteps.playlist.name,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        "${playlistWithSteps.steps.size} steps",
                        color = Color(0xFF888888),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onPlay) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF00C853),
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Mini step strip
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                items(playlistWithSteps.steps.sortedBy { it.stepIndex }) { step ->
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFF3A3A3A), RoundedCornerShape(4.dp))
                            .background(Color(0xFF222222)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                            repeat(6) { i ->
                                val intensity = step.channelIntensities[getChannelForIndex(i)] ?: 0
                                Box(
                                    modifier = Modifier
                                        .size(width = 3.dp, height = 8.dp)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(intensityColor[intensity])
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = Color(0xFF666666), fontSize = 11.sp, modifier = Modifier.fillMaxWidth())
}

fun getChannelForIndex(index: Int): Int = when (index) {
    0 -> Glyph.Code_25111.A_1
    1 -> Glyph.Code_25111.A_2
    2 -> Glyph.Code_25111.A_3
    3 -> Glyph.Code_25111.A_4
    4 -> Glyph.Code_25111.A_5
    5 -> Glyph.Code_25111.A_6
    else -> 0
}

// ─── Previews ────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
fun GlyphBoxIntensityPreview() {
    GlyphBarComposerTheme {
        Row(
            Modifier
                .background(Color(0xFF0E0E0E))
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlyphBox(label = "A1", intensity = 0, onIntensityChange = {})
            GlyphBox(label = "A2", intensity = 1, onIntensityChange = {})
            GlyphBox(label = "A3", intensity = 2, onIntensityChange = {})
            GlyphBox(label = "A4", intensity = 3, onIntensityChange = {})
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StepPreviewBoxPreview() {
    GlyphBarComposerTheme {
        Box(
            Modifier
                .background(Color(0xFF0E0E0E))
                .padding(16.dp)
        ) {
            StepPreviewBox(
                step = GlyphSequence(
                    channelIntensities = mapOf(
                        Glyph.Code_25111.A_1 to 1,
                        Glyph.Code_25111.A_2 to 2,
                        Glyph.Code_25111.A_3 to 3,
                        Glyph.Code_25111.A_4 to 0
                    ),
                    durationMs = 1000
                )
            )
        }
    }
}