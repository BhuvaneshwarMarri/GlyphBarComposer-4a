package com.smaarig.glyphbarcomposer.controller

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.ui.widget.GlyphComposerHorizontalWidget
import com.smaarig.glyphbarcomposer.ui.widget.GlyphComposerVerticalWidget
import com.smaarig.glyphbarcomposer.ui.widget.INTENSITIES_KEY
import com.smaarig.glyphbarcomposer.ui.widget.updateAllWidgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GlyphController private constructor() {
    enum class GlyphOwner { NONE, COMPOSER, STUDIO, PATTERN_LAB, BATTERY }

    private val _activeOwner = MutableStateFlow(GlyphOwner.NONE)
    val activeOwner = _activeOwner.asStateFlow()

    private var mGlyphManager: GlyphManager? = null
    private var mContext: Context? = null
    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null
    private var batteryJob: Job? = null
    private var playbackJob: Job? = null
    private var lastWidgetUpdateMs = 0L
    @Volatile private var isDeinitialized = false

    // ── Global State for Preview ──────────────────────────────────────────
    private val _currentIntensities = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0, 0))
    val currentIntensities = _currentIntensities.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused = _isPaused.asStateFlow()

    // ── Global State for Battery Feature ──────────────────────────────────
    private val _isBatteryFeatureEnabled = MutableStateFlow(false)
    val isBatteryFeatureEnabled = _isBatteryFeatureEnabled.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0)
    private val _isCharging = MutableStateFlow(false)
    private val _isHardwareBusy = MutableStateFlow(false)

    val channels = GlyphConstants.PHONE_4A_CHANNELS

    init {
        // Start battery monitoring loop
        controllerScope.launch {
            combine(
                _isBatteryFeatureEnabled,
                _isCharging,
                _batteryLevel,
                _isHardwareBusy
            ) { enabled, charging, level, busy -> Quadruple(enabled, charging, level, busy) }
                .collect { (enabled, charging, level, busy) ->
                    Log.d(
                        TAG,
                        "Battery Update: enabled=$enabled, charging=$charging, level=$level, busy=$busy"
                    )
                    if (enabled && charging && !busy) {
                        // Start battery visualization (it will auto-stop after 3s in the function)
                        startBatteryVisualization(level)
                    } else if (!charging && batteryJob != null) {
                        stopBatteryVisualization()
                    }
                }
        }
    }

    private data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    fun updateBatteryProgress(level: Int, charging: Boolean) {
        _batteryLevel.value = level
        _isCharging.value = charging
        Log.d(TAG, "updateBatteryProgress: level=$level, charging=$charging")
    }

    fun showBatteryPeek(durationMs: Long = 3000) {
        if (!_isBatteryFeatureEnabled.value) return
        startBatteryVisualization(_batteryLevel.value, durationMs)
    }

    private fun startBatteryVisualization(level: Int, autoOffDelay: Long = 3000) {
        if (isDeinitialized) return
        batteryJob?.cancel()
        batteryJob = controllerScope.launch {
            if (!_isHardwareBusy.value) {
                // Smooth progress (18 steps: 6 segments * 3 intensities)
                val progressIndex = (level * 18 / 100).coerceIn(0, 18)
                val fullSegments = progressIndex / 3
                val partialIntensity = progressIndex % 3

                val reversedChannels = channels.take(6).reversed() // [A6, A5, A4, A3, A2, A1]
                val intensities = mutableMapOf<Int, Int>()

                reversedChannels.forEachIndexed { index, ch ->
                    intensities[ch] = when {
                        index < fullSegments -> 3
                        index == fullSegments && partialIntensity > 0 -> partialIntensity
                        else -> 0
                    }
                }

                // Red glyph (7th channel) represents 15% charging
                val redGlyph = channels[6]
                intensities[redGlyph] = if (level <= 15) 3 else 0

                // Update preview and hardware
                val previewList = channels.map { ch -> intensities[ch] ?: 0 }
                _currentIntensities.value = previewList

                try {
                    val sdkIntensities =
                        channels.map { ch -> stateToSdkIntensity(intensities[ch] ?: 0) }
                    val frameColors =
                        IntArray(7) { i -> if (i < sdkIntensities.size) sdkIntensities[i] else 0 }
                    mGlyphManager?.setFrameColors(frameColors)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in battery visualization: ${e.message}")
                }

                // Auto-off after specific delay (default 3s)
                delay(autoOffDelay)
                stopBatteryVisualization()
            }
        }
    }

    private fun stopBatteryVisualization() {
        batteryJob?.cancel()
        batteryJob = null
        _currentIntensities.value = List(7) { 0 }
        // Always turn off hardware regardless of busy state.
        if (mGlyphManager == null) return
        try {
            mGlyphManager?.turnOff()
            mGlyphManager?.setFrameColors(IntArray(7) { 0 })
        } catch (e: Exception) {
            Log.e(TAG, "Error turning off battery visualization: ${e.message}")
        }
        Log.d(TAG, "Battery Visualization Stopped")
    }

    fun toggleBatteryFeature(enabled: Boolean) {
        _isBatteryFeatureEnabled.value = enabled
    }

    fun acquireControl(owner: GlyphOwner) {
        if (_activeOwner.value != GlyphOwner.NONE && _activeOwner.value != owner) {
            stopPlayback() // gracefully stop previous owner
        }
        _activeOwner.value = owner
    }

    fun releaseControl(owner: GlyphOwner) {
        if (_activeOwner.value == owner) {
            _activeOwner.value = GlyphOwner.NONE
        }
    }

    private fun maybeUpdateWidgets(
        context: Context,
        intensities: List<Int>,
        isPlaying: Boolean? = null,
        playlistId: Long? = null,
        playlistName: String? = null,
        forceUpdate: Boolean = false
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        // Reduced throttle to 250ms for better perceived sync.
        // Always force update if isPlaying state is changing.
        if (forceUpdate || isPlaying != null || now - lastWidgetUpdateMs > 250L) {
            updateAllWidgets(
                context,
                intensities = intensities,
                isPlaying = isPlaying,
                playlistId = playlistId,
                playlistName = playlistName
            )
            lastWidgetUpdateMs = now
        }
    }

    companion object {
        private const val TAG = "GlyphController"

        @Volatile
        private var sInstance: GlyphController? = null

        @JvmStatic
        @Synchronized
        fun getInstance(context: Context): GlyphController {
            if (sInstance == null) {
                sInstance = GlyphController()
            }
            sInstance!!.init(context.applicationContext)
            return sInstance!!
        }

        /**
         * Maps the app's 0–3 intensity scale to the Nothing Glyph SDK's 0–4095 scale.
         *   0 = off
         *   1 = low glow  → 500  (~12%)
         *   2 = medium    → 1500 (~37%)
         *   3 = full      → 4000 (~98%, matching Nothing's DEFAULT_LIGHT constant)
         *
         * Values ≥ 10 are passed through directly (raw SDK values from MULTI_BAND algorithm).
         */
        private fun stateToSdkIntensity(state: Int): Int = when {
            state >= 10 -> state.coerceIn(0, 4095)
            state == 3 -> 4000
            state == 2 -> 1500
            state == 1 -> 500
            else -> 0
        }
    }

    private val mCallback = object : GlyphManager.Callback {
        override fun onServiceConnected(componentName: ComponentName) {
            mGlyphManager?.let {
                when {
                    Common.is25111() -> {
                        it.register(Glyph.DEVICE_25111)
                        Log.d(TAG, "Registered for Phone (4a)")
                    }

                    Common.is20111() -> it.register(Glyph.DEVICE_20111)
                    Common.is22111() -> it.register(Glyph.DEVICE_22111)
                    Common.is23111() -> it.register(Glyph.DEVICE_23111)
                    Common.is23113() -> it.register(Glyph.DEVICE_23113)
                    Common.is24111() -> it.register(Glyph.DEVICE_24111)
                    else -> it.register()
                }

                try {
                    it.openSession()
                    Log.d(TAG, "Glyph Session Opened")
                } catch (e: GlyphException) {
                    Log.e(TAG, "Failed to open session: ${e.message}")
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            try {
                mGlyphManager?.closeSession()
            } catch (e: GlyphException) {
                Log.e(TAG, "Failed to close session on disconnect: ${e.message}")
            }
        }
    }

    fun init(context: Context) {
        mContext = context.applicationContext
        if (mGlyphManager == null) {
            mGlyphManager = GlyphManager.getInstance(context)
            mGlyphManager?.init(mCallback)
            // BUG-6 FIX: Only sync widgets once on first init, not on every getInstance() call.
            // Moving this inside the null-check ensures it runs only when the manager is
            // freshly created, preventing unnecessary widget blasts on subsequent calls.
            updateAllWidgets(context.applicationContext, intensities = _currentIntensities.value)
        }
    }

    fun turnOffGlyphs() {
        stopPlayback()
        batteryJob?.cancel()
        batteryJob = null
        resetJob?.cancel()
        resetJob = null

        if (mGlyphManager == null) return
        try {
            mGlyphManager?.turnOff()
            // Safety: also reset via setFrameColors
            mGlyphManager?.setFrameColors(IntArray(7) { 0 })
        } catch (e: Exception) {
        }
        _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
        _isHardwareBusy.value = false

        // Sync with widgets
        mContext?.let { context ->
            maybeUpdateWidgets(context, intensities = _currentIntensities.value, isPlaying = false, forceUpdate = true)
        }
    }

    fun setRedGlyph(state: Int) {
        if (isDeinitialized) return
        // Red glyph usually comes from manual composer or battery, we allow it if no other owner holds control
        // or if it's the composer. For simplicity in this legacy method, we check ownership.
        if (_activeOwner.value != GlyphOwner.NONE && _activeOwner.value != GlyphOwner.COMPOSER) return
        if (mGlyphManager == null) return

        val intensities = _currentIntensities.value.toMutableList()
        if (intensities.size >= 7) {
            intensities[6] = state
            _currentIntensities.value = intensities
        }

        try {
            val sdkIntensity = stateToSdkIntensity(state)

            // BUG-8 FIX: Renamed inner variable from `intensities` to `currentSnapshot`
            // to avoid shadowing the outer mutable list and make the intent explicit.
            val currentSnapshot = _currentIntensities.value
            val frameColors = IntArray(7) { i ->
                if (i == 6) sdkIntensity
                else if (i < currentSnapshot.size) stateToSdkIntensity(currentSnapshot[i])
                else 0
            }
            mGlyphManager?.setFrameColors(frameColors)

            Log.d(
                TAG,
                "Red Glyph Set via setFrameColors: $sdkIntensity, Full: ${frameColors.contentToString()}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error in setRedGlyph: ${e.message}")
        }

        // Sync with widgets
        mContext?.let { context ->
            maybeUpdateWidgets(context, intensities = _currentIntensities.value)
        }
    }

    fun applyGlyphStateWithIntensities(channelIntensities: Map<Int, Int>, durationMs: Int, owner: GlyphOwner = GlyphOwner.NONE) {
        if (isDeinitialized) return
        if (mGlyphManager == null) {
            Log.e(TAG, "applyGlyphStateWithIntensities: GlyphManager is null")
            return
        }

        // Reject write if a different owner currently holds control
        val active = _activeOwner.value
        if (active != GlyphOwner.NONE && active != owner) return

        if (!_isPlaying.value) {
            _isHardwareBusy.value = true
            batteryJob?.cancel()
        }

        // Update Global State for Preview
        val newIntensities = channels.map { ch -> channelIntensities[ch] ?: 0 }
        _currentIntensities.value = newIntensities

        // Sync with widgets
        mContext?.let { context ->
            maybeUpdateWidgets(context, intensities = newIntensities)
        }

        // Auto-reset hardware busy state after duration, but DON'T reset _currentIntensities
        // unless they are all zero. The preview should show the live state.
        if (!_isPlaying.value) {
            resetJob?.cancel()
            resetJob = controllerScope.launch {
                delay(durationMs.toLong())
                _isHardwareBusy.value = false
            }
        }

        if (mGlyphManager == null) return
        try {
            // Using setFrameColors for the entire 7-glyph set to ensure perfect hardware sync
            val frameColors = IntArray(7) { i ->
                if (i < newIntensities.size) stateToSdkIntensity(newIntensities[i]) else 0
            }
            mGlyphManager?.setFrameColors(frameColors)

            Log.i(TAG, "Glyph Frame via setFrameColors: ${frameColors.contentToString()}")

            // If all are zero, we explicitly turn off to be safe
            if (newIntensities.all { it == 0 }) {
                mGlyphManager?.turnOff()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyGlyphStateWithIntensities: ${e.message}", e)
        }
    }

    fun playSequence(steps: List<GlyphSequence>, loop: Boolean = false, name: String? = null, id: Long? = null, owner: GlyphOwner = GlyphOwner.NONE) {
        if (isDeinitialized) return
        if (steps.isEmpty()) return

        // Ownership check
        if (_activeOwner.value != GlyphOwner.NONE && _activeOwner.value != owner) return

        stopPlayback()

        _isPlaying.value = true
        _isHardwareBusy.value = true

        playbackJob = controllerScope.launch {
            try {
                // Sync start of playback to widgets
                mContext?.let { context ->
                    maybeUpdateWidgets(context, isPlaying = true, playlistId = id, playlistName = name, intensities = _currentIntensities.value, forceUpdate = true)
                }

                var expectedTimeMs = android.os.SystemClock.elapsedRealtime()

                do {
                    for (step in steps) {
                        while (_isPaused.value) {
                            delay(50)
                            expectedTimeMs = android.os.SystemClock.elapsedRealtime()
                        }

                        applyGlyphStateWithIntensities(step.channelIntensities, step.durationMs, owner)
                        expectedTimeMs += step.durationMs.toLong()
                        val remaining = expectedTimeMs - android.os.SystemClock.elapsedRealtime()
                        if (remaining > 0) {
                            delay(remaining)
                        }
                    }
                } while (loop && isActive)
            } finally {
                if (!loop || !isActive) {
                    stopPlayback()
                }
            }
        }
    }

    fun stopPlayback() {
        playbackJob?.cancel()
        playbackJob = null
        _isPlaying.value = false
        _isPaused.value = false
        _isHardwareBusy.value = false

        // Don't turn off if hardware is busy with a single manual trigger
        if (resetJob == null && mGlyphManager != null) {
            try {
                mGlyphManager?.turnOff()
                mGlyphManager?.setFrameColors(IntArray(7) { 0 })
            } catch (e: Exception) {
            }
            _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
            mContext?.let { maybeUpdateWidgets(it, intensities = _currentIntensities.value, isPlaying = false, forceUpdate = true) }
        }
    }

    fun togglePausePlayback() {
        if (_isPlaying.value) {
            _isPaused.value = !_isPaused.value
            mContext?.let { context ->
                maybeUpdateWidgets(context, intensities = _currentIntensities.value, isPlaying = !_isPaused.value, forceUpdate = true)
            }
        }
    }

    fun applyGlyphState(activeChannels: List<Int>, durationMs: Int) {
        if (isDeinitialized) return
        val intensityMap = channels.associate { ch ->
            ch to if (activeChannels.contains(ch)) 3 else 0
        }
        applyGlyphStateWithIntensities(intensityMap, durationMs, GlyphOwner.COMPOSER)
    }

    fun applySmoothProgress(percentage: Int) {
        if (isDeinitialized) return
        if (mGlyphManager == null) return

        // Smooth progress is often used by Battery or special features.
        // If Battery holds control, it's fine.
        if (_activeOwner.value != GlyphOwner.NONE && _activeOwner.value != GlyphOwner.BATTERY) return

        // 18 steps total across 6 glyphs: 6 segments * 3 intensities (or continuous intensity)
        // For truly smooth, we map 0-100 to 0 - (6 * 4095)
        val totalRange = 6 * 4095
        val progress = (percentage * totalRange / 100).coerceIn(0, totalRange)

        val fullSegments = progress / 4095
        val partialIntensity = progress % 4095

        val reversedChannels = channels.take(6).reversed() // [A6, A5, A4, A3, A2, A1]
        val intensities = mutableMapOf<Int, Int>()

        reversedChannels.forEachIndexed { index, ch ->
            intensities[ch] = when {
                index < fullSegments -> 4095
                index == fullSegments -> partialIntensity
                else -> 0
            }
        }

        // Update Global State for Preview (approximate intensities for preview 0-3).
        // channels has 7 elements [A1..A6, RED]. The smooth-progress intensities map only
        //populates A1..A6; RED is absent, so `intensities[redCh] ?: 0` = 0.
        // We preserve the existing RED state rather than zeroing it.
        val redState = if (_currentIntensities.value.size >= 7) _currentIntensities.value[6] else 0
        val previewList = channels.mapIndexed { idx, ch ->
            if (idx == 6) {
                redState
            } else {
                val raw = intensities[ch] ?: 0
                if (raw == 0) 0 else if (raw < 1365) 1 else if (raw < 2730) 2 else 3
            }
        }
        _currentIntensities.value = previewList

        if (mGlyphManager == null) return
        try {
            val frameColors = IntArray(7) { i ->
                if (i < 6) {
                    val ch = channels[i]
                    intensities[ch] ?: 0
                } else {
                    // Red glyph sync (7th channel, index 6)
                    stateToSdkIntensity(redState)
                }
            }
            mGlyphManager?.setFrameColors(frameColors)
        } catch (e: Exception) {
            Log.e(TAG, "Error in smooth progress: ${e.message}")
        }
    }

    fun deinit() {
        isDeinitialized = true
        controllerScope.cancel()
        mGlyphManager?.let {
            try {
                it.closeSession()
                Log.d(TAG, "Glyph Session Closed")
            } catch (e: GlyphException) {
                Log.e(TAG, "Failed to close session in deinit: ${e.message}")
            }
            try {
                it.unInit()
                Log.d(TAG, "GlyphManager Uninitialized")
            } catch (e: Exception) {
                Log.e(TAG, "Error in GlyphManager unInit: ${e.message}")
            }
            mGlyphManager = null
            _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
        }
    }
}