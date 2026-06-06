package com.smaarig.glyphbarcomposer.controller

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smaarig.glyphbarcomposer.model.GlyphSequence
import com.smaarig.glyphbarcomposer.service.GlyphPlaybackService
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
    enum class GlyphOwner { NONE, COMPOSER, STUDIO, PATTERN_LAB, BATTERY, WIDGET }

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
    private var isBridgeInitialized = false

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
        playlistName: String? = null
    ) {
        val now = android.os.SystemClock.elapsedRealtime()
        // Re-introduce a lightweight throttle to prevent flooding Glance
        if (isPlaying != null || now - lastWidgetUpdateMs > 200L) {
            controllerScope.launch {
                updateAllWidgets(
                    context,
                    intensities = intensities,
                    isPlaying = isPlaying,
                    playlistId = playlistId,
                    playlistName = playlistName
                )
            }
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
            val instance = sInstance ?: GlyphController().also { sInstance = it }
            instance.init(context.applicationContext)
            return instance
        }

        /**
         * Maps the app's intensity states to the Nothing Glyph SDK's 0–4095 scale.
         * White states: 1 (Low), 2 (Med), 3 (High)
         * Red states:   4 (Low), 5 (Med), 6 (High)
         * Values ≥ 10 are passed through directly (raw SDK values).
         */
        private fun stateToSdkIntensity(state: Int): Int = when {
            state >= 10 -> state.coerceIn(0, 4095)
            state == 3 || state == 6 -> 4000
            state == 2 || state == 5 -> 1500
            state == 1 || state == 4 -> 500
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
        val appContext = context.applicationContext
        mContext = appContext
        if (mGlyphManager == null) {
            mGlyphManager = GlyphManager.getInstance(appContext)
            mGlyphManager?.init(mCallback)
        }

        // Bridge: listen to service-driven playback state
        if (!isBridgeInitialized) {
            LocalBroadcastManager.getInstance(appContext).registerReceiver(
                object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        val isPlaying = intent.getBooleanExtra(GlyphPlaybackService.EXTRA_IS_PLAYING, false)
                        _isPlaying.value = isPlaying
                        if (!isPlaying) _currentIntensities.value = List(7) { 0 }
                    }
                },
                IntentFilter(GlyphPlaybackService.ACTION_PLAYBACK_STATE_CHANGED)
            )
            isBridgeInitialized = true
        }
        
        // Ensure widgets are in sync with current controller state on init
        controllerScope.launch {
            updateAllWidgets(appContext, intensities = _currentIntensities.value, isPlaying = _isPlaying.value)
        }
    }

    /**
     * Force-sync the controller's internal state to match external values.
     * Useful during cold-starts triggered by widgets.
     */
    fun restoreStateFromWidget(intensities: List<Int>) {
        if (intensities.size == 7) {
            _currentIntensities.value = intensities
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
        val offIntensities = listOf(0, 0, 0, 0, 0, 0, 0)
        _currentIntensities.value = offIntensities
        _isHardwareBusy.value = false

        // Sync with widgets
        mContext?.let { context ->
            controllerScope.launch {
                updateAllWidgets(context, intensities = offIntensities, isPlaying = false)
            }
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

            // Using the undocumented API suggested by user for red glyph hardware sync
            // We pass all current intensities to keep them in sync
            val intensities = _currentIntensities.value
            val frameColors = IntArray(7) { i ->
                if (i == 6) sdkIntensity
                else if (i < intensities.size) stateToSdkIntensity(intensities[i])
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

    fun applyGlyphStateWithIntensities(
        channelIntensities: Map<Int, Int>,
        durationMs: Int,
        owner: GlyphOwner = GlyphOwner.NONE
    ) {
        if (isDeinitialized) return
        
        // 1. Ownership check - Widgets are allowed to bypass for "Preview" behavior
        val active = _activeOwner.value
        if (active != GlyphOwner.NONE && active != owner && owner != GlyphOwner.WIDGET) return

        // 2. Hardware Trigger (Highest Priority for latency)
        val newIntensities = channels.map { ch -> channelIntensities[ch] ?: 0 }
        mGlyphManager?.let { manager ->
            try {
                val frameColors = IntArray(7) { i ->
                    if (i < newIntensities.size) stateToSdkIntensity(newIntensities[i]) else 0
                }
                manager.setFrameColors(frameColors)
                if (newIntensities.all { it == 0 }) {
                    manager.turnOff()
                }
                Log.d(TAG, "Hardware Frame sync: ${frameColors.contentToString()}")
            } catch (e: Exception) {
                Log.e(TAG, "Hardware sync error: ${e.message}")
            }
        } ?: Log.w(TAG, "applyGlyphStateWithIntensities: GlyphManager is null, hardware skipped")

        // 3. Internal State Update
        _currentIntensities.value = newIntensities
        if (!_isPlaying.value) {
            _isHardwareBusy.value = true
            batteryJob?.cancel()
            resetJob?.cancel()
            resetJob = controllerScope.launch {
                delay(durationMs.toLong())
                _isHardwareBusy.value = false
            }
        }

        // 4. Widget Sync (Lower priority, throttled)
        mContext?.let { context ->
            maybeUpdateWidgets(context, intensities = newIntensities)
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
                    updateAllWidgets(context, isPlaying = true, playlistId = id, playlistName = name, intensities = _currentIntensities.value)
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
            val offIntensities = listOf(0, 0, 0, 0, 0, 0, 0)
            _currentIntensities.value = offIntensities
            mContext?.let { context ->
                controllerScope.launch {
                    updateAllWidgets(context, intensities = offIntensities, isPlaying = false)
                }
            }
        }
    }

    fun togglePausePlayback() {
        if (_isPlaying.value) {
            _isPaused.value = !_isPaused.value
            mContext?.let { context ->
                controllerScope.launch {
                    updateAllWidgets(context, intensities = _currentIntensities.value, isPlaying = !_isPaused.value)
                }
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

        // Update Global State for Preview (approximate intensities for preview 0-3)
        val previewList = channels.map { ch ->
            val raw = intensities[ch] ?: 0
            if (raw == 0) 0 else if (raw < 1365) 1 else if (raw < 2730) 2 else 3
        }.toMutableList()
        // Keep red glyph state from current intensities
        if (_currentIntensities.value.size >= 7) {
            previewList.add(_currentIntensities.value[6])
        } else {
            previewList.add(0)
        }
        _currentIntensities.value = previewList

        if (mGlyphManager == null) return
        try {
            val frameColors = IntArray(7) { i ->
                if (i < 6) {
                    val ch = channels[i]
                    intensities[ch] ?: 0
                } else {
                    // Red glyph sync
                    val state =
                        if (_currentIntensities.value.size >= 7) _currentIntensities.value[6] else 0
                    stateToSdkIntensity(state)
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
        sInstance = null
    }
}
