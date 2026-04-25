package com.smaarig.glyphbarcomposer.controller

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import com.smaarig.glyphbarcomposer.service.BatteryMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine

class GlyphController private constructor() {
    private var mGlyphManager: GlyphManager? = null
    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null
    private var batteryJob: Job? = null

    // ── Global State for Preview ──────────────────────────────────────────
    private val _currentIntensities = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0, 0))
    val currentIntensities = _currentIntensities.asStateFlow()

    // ── Global State for Battery Feature ──────────────────────────────────
    private val _isBatteryFeatureEnabled = MutableStateFlow(false)
    val isBatteryFeatureEnabled = _isBatteryFeatureEnabled.asStateFlow()

    private val _batteryLevel = MutableStateFlow(0)
    private val _isCharging = MutableStateFlow(false)
    private val _isHardwareBusy = MutableStateFlow(false)

    private val channels = listOf(
        Glyph.Code_25111.A_1, Glyph.Code_25111.A_2, Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4, Glyph.Code_25111.A_5, Glyph.Code_25111.A_6,
        Glyph.Code_22111.E1 // Red glyph (channel 24)
    )

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
                    Log.d(TAG, "Battery Update: enabled=$enabled, charging=$charging, level=$level, busy=$busy")
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
                    mGlyphManager?.openSession()
                    val sdkIntensities = channels.map { ch -> stateToSdkIntensity(intensities[ch] ?: 0) }
                    val frameColors = IntArray(7) { i -> if (i < sdkIntensities.size) sdkIntensities[i] else 0 }
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
        _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
        if (!_isHardwareBusy.value) {
            turnOffGlyphs()
        }
        Log.d(TAG, "Battery Visualization Stopped")
    }

    fun toggleBatteryFeature(enabled: Boolean) {
        _isBatteryFeatureEnabled.value = enabled
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
            sInstance!!.init(context.getApplicationContext())
            return sInstance!!
        }

        private fun stateToSdkIntensity(state: Int): Int {
            return when (state) {
                1, 4 -> 500   // Low
                2, 5 -> 1500  // Medium
                3, 6 -> 4000  // High / Full (SDK DEFAULT_LIGHT)
                else -> 0
            }
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
        if (mGlyphManager == null) {
            mGlyphManager = GlyphManager.getInstance(context)
            mGlyphManager?.init(mCallback)
        }
    }

    fun turnOffGlyphs() {
        try {
            mGlyphManager?.openSession()
            mGlyphManager?.turnOff()
            // Safety: also reset via setFrameColors
            mGlyphManager?.setFrameColors(IntArray(7) { 0 })
        } catch (e: Exception) {}
        _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0, 0)
    }

    fun setRedGlyph(state: Int) {
        val intensities = _currentIntensities.value.toMutableList()
        if (intensities.size >= 7) {
            intensities[6] = state
            _currentIntensities.value = intensities
        }
        
        try {
            mGlyphManager?.openSession()
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
            
            Log.d(TAG, "Red Glyph Set via setFrameColors: $sdkIntensity, Full: ${frameColors.contentToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error in setRedGlyph: ${e.message}")
        }
    }

    fun applyGlyphStateWithIntensities(channelIntensities: Map<Int, Int>, durationMs: Int) {
        if (mGlyphManager == null) {
            Log.e(TAG, "applyGlyphStateWithIntensities: GlyphManager is null")
            return
        }

        _isHardwareBusy.value = true
        batteryJob?.cancel()

        // Update Global State for Preview
        val newIntensities = channels.map { ch -> channelIntensities[ch] ?: 0 }
        _currentIntensities.value = newIntensities

        // Auto-reset hardware busy state after duration, but DON'T reset _currentIntensities
        // unless they are all zero. The preview should show the live state.
        resetJob?.cancel()
        resetJob = controllerScope.launch {
            delay(durationMs.toLong())
            _isHardwareBusy.value = false
        }

        try {
            mGlyphManager?.openSession()
            
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

    fun applyGlyphState(activeChannels: List<Int>, durationMs: Int) {
        if (mGlyphManager == null) return

        _isHardwareBusy.value = true
        batteryJob?.cancel()

        // Update Global State for Preview (legacy mode)
        val newIntensities = channels.map { ch -> if (activeChannels.contains(ch)) 3 else 0 }
        _currentIntensities.value = newIntensities

        // Auto-reset busy state only
        resetJob?.cancel()
        resetJob = controllerScope.launch {
            delay(durationMs.toLong())
            _isHardwareBusy.value = false
        }

        try {
            mGlyphManager?.openSession()
            
            // Map active channels to intensities for setFrameColors
            val frameColors = IntArray(7) { i ->
                if (activeChannels.contains(channels[i])) 255 else 0
            }
            mGlyphManager?.setFrameColors(frameColors)
            
            if (activeChannels.isEmpty()) {
                mGlyphManager?.turnOff()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyGlyphState: ${e.message}")
        }
    }

    fun deinit() {
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
