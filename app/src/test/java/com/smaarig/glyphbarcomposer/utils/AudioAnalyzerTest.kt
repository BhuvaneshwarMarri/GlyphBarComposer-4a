package com.smaarig.glyphbarcomposer.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudioAnalyzerTest {

    @Test
    fun `buildHannWindow produces correct values`() {
        val window = AudioAnalyzer.buildHannWindow(1024)
        assertEquals(1024, window.size)
        assertEquals(0f, window[0], 0.001f)
        assertEquals(1f, window[512], 0.001f)
    }

    @Test
    fun `performFFT produces correct frequency components`() {
        val size = 1024
        val samples = ShortArray(size) { i ->
            // Sine wave at bin 10
            (32767 * Math.sin(2.0 * Math.PI * 10.0 * i / size)).toInt().toShort()
        }
        val window = FloatArray(size) { 1f } // rectangular window for simple test
        
        val fft = AudioAnalyzer.performFFT(samples, window)
        
        // Find peak
        var maxIdx = 0
        for (i in fft.indices) {
            if (fft[i] > fft[maxIdx]) maxIdx = i
        }
        
        assertEquals(10, maxIdx)
        assertTrue(fft[10] > 0.5f)
    }
}
