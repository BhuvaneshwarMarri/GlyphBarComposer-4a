package com.smaarig.glyphbarcomposer.controller

import android.content.ComponentName
import android.content.Context
import android.util.Log
import com.nothing.ketchum.Common
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class GlyphController private constructor() {
    private var mGlyphManager: GlyphManager? = null
    private val controllerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var resetJob: Job? = null

    // ── Global State for Preview ──────────────────────────────────────────
    private val _currentIntensities = MutableStateFlow(listOf(0, 0, 0, 0, 0, 0))
    val currentIntensities = _currentIntensities.asStateFlow()

    private val channels = listOf(
        Glyph.Code_25111.A_1, Glyph.Code_25111.A_2, Glyph.Code_25111.A_3,
        Glyph.Code_25111.A_4, Glyph.Code_25111.A_5, Glyph.Code_25111.A_6
    )

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
                1 -> 100
                2 -> 180
                3 -> 255
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
        mGlyphManager?.turnOff()
        _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0)
    }

    fun applyGlyphStateWithIntensities(channelIntensities: Map<Int, Int>, durationMs: Int) {
        if (mGlyphManager == null) {
            Log.e(TAG, "applyGlyphStateWithIntensities: GlyphManager is null")
            return
        }

        // Update Global State for Preview
        val newIntensities = channels.map { ch -> channelIntensities[ch] ?: 0 }
        _currentIntensities.value = newIntensities

        // Auto-reset preview after duration
        resetJob?.cancel()
        resetJob = controllerScope.launch {
            delay(durationMs.toLong())
            if (_currentIntensities.value == newIntensities) {
                _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0)
            }
        }

        try {
            val builder = mGlyphManager!!.glyphFrameBuilder
            var anyActive = false

            for ((channel, state) in channelIntensities) {
                val sdkIntensity = stateToSdkIntensity(state)
                if (sdkIntensity > 0) {
                    anyActive = true
                    if (state == 3 || sdkIntensity >= 255) {
                        builder.buildChannel(channel)
                    } else {
                        builder.buildChannel(channel, sdkIntensity)
                    }
                }
            }

            if (!anyActive) {
                mGlyphManager?.turnOff()
                return
            }

            try {
                mGlyphManager?.openSession()
            } catch (e: Exception) {}

            builder.buildPeriod(durationMs)
            mGlyphManager?.toggle(builder.build())
            Log.i(TAG, "Glyph Frame Applied: $channelIntensities")
        } catch (e: Exception) {
            Log.e(TAG, "Error in applyGlyphStateWithIntensities: ${e.message}", e)
        }
    }

    fun applyGlyphState(activeChannels: List<Int>, durationMs: Int) {
        if (mGlyphManager == null) return

        // Update Global State for Preview (legacy mode)
        val newIntensities = channels.map { ch -> if (activeChannels.contains(ch)) 3 else 0 }
        _currentIntensities.value = newIntensities

        // Auto-reset preview after duration
        resetJob?.cancel()
        resetJob = controllerScope.launch {
            delay(durationMs.toLong())
            if (_currentIntensities.value == newIntensities) {
                _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0)
            }
        }

        try {
            if (activeChannels.isEmpty()) {
                mGlyphManager?.turnOff()
                return
            }

            try {
                mGlyphManager?.openSession()
            } catch (e: Exception) {}

            val builder = mGlyphManager!!.glyphFrameBuilder
            for (channel in activeChannels) {
                builder.buildChannel(channel)
            }
            builder.buildPeriod(durationMs)
            mGlyphManager?.toggle(builder.build())
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
            _currentIntensities.value = listOf(0, 0, 0, 0, 0, 0)
        }
    }
}
