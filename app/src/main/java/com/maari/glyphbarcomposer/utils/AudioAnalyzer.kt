package com.maari.glyphbarcomposer.utils

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.util.Log
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object AudioAnalyzer {
    private const val TAG = "AudioAnalyzer"
    private const val FFT_SIZE = 1024
    private const val SAMPLE_RATE = 44100
    private const val HZ_PER_BIN = SAMPLE_RATE.toFloat() / FFT_SIZE

    private const val RELEASE_RATE = 0.82f
    private const val PEAK_FALLOFF = 0.996f

    fun analyzeAdvancedDSP(
        context: Context,
        uri: Uri,
        durationMs: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = mutableListOf<Map<Int, Int>>()
        val hannWindow = buildHannWindow(FFT_SIZE)
        val bandMagnitudes = FloatArray(6)
        val adaptivePeaks = FloatArray(6) { 0.1f }
        val currentLevels = IntArray(6)
        var prevFft: FloatArray? = null

        for (window in windows) {
            val fft = performFFT(window, hannWindow)
            val intensities =
                processWhiteLeds(fft, bandMagnitudes, adaptivePeaks, currentLevels, channels)
            val redVal = processRedOnset(fft, prevFft)
            prevFft = fft.copyOf()
            val map = intensities.toMutableMap()
            if (redVal > 0) map[channels[6]] = redVal
            result += map
        }
        result
    }

    fun analyzePeakDetection(
        context: Context,
        uri: Uri,
        durationMs: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = mutableListOf<Map<Int, Int>>()
        val hannWindow = buildHannWindow(FFT_SIZE)

        val histSize = 30
        val histories = Array(6) { ArrayDeque<Float>(histSize) }
        val peakHold = FloatArray(6) { 0f }
        val PEAK_HOLD_DECAY = 0.90f

        val lastFired = IntArray(6) { -20 }
        val REFRACTORY_WINDOWS = 4

        var prevSubBass = 0f

        windows.forEachIndexed { idx, window ->
            val fft = performFFT(window, hannWindow)
            val map = mutableMapOf<Int, Int>()

            for (b in 0..5) {
                val freq = bandEnergy(fft, b)

                histories[b].addLast(freq)
                if (histories[b].size > histSize) histories[b].removeFirst()

                val median = histories[b].sorted().let { s -> s[s.size / 2] }
                val threshold = median * 2.2f

                peakHold[b] = maxOf(freq, peakHold[b] * PEAK_HOLD_DECAY)

                val isOnset = freq > threshold &&
                        freq > 0.03f &&
                        idx - lastFired[b] >= REFRACTORY_WINDOWS

                if (isOnset) {
                    lastFired[b] = idx
                    val level = when {
                        freq > peakHold[b] * 0.85f -> 3
                        freq > peakHold[b] * 0.55f -> 2
                        else -> 1
                    }
                    map[channels[b]] = level
                }
            }

            val subBass = bandEnergyHz(fft, 20f, 100f)
            val flux = (subBass - prevSubBass).coerceAtLeast(0f)
            if (flux > 0.08f) map[channels[6]] = if (flux > 0.20f) 3 else if (flux > 0.12f) 2 else 1
            prevSubBass = subBass

            result += map
        }
        result
    }

    fun analyzeSpectralFlux(
        context: Context,
        uri: Uri,
        durationMs: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = mutableListOf<Map<Int, Int>>()
        val hannWindow = buildHannWindow(FFT_SIZE)
        val histSize = 43

        val fluxHist = Array(6) { ArrayDeque<Float>(histSize) }
        var prevFft: FloatArray? = null

        val hfFluxHist = ArrayDeque<Float>(histSize)
        var prevHfEnergy = 0f

        windows.forEach { window ->
            val fft = performFFT(window, hannWindow)
            val map = mutableMapOf<Int, Int>()
            val prev = prevFft

            for (b in 0..5) {
                val currEnergy = bandEnergy(fft, b)
                val prevEnergy = if (prev != null) bandEnergy(prev, b) else 0f
                val flux = (currEnergy - prevEnergy).coerceAtLeast(0f)

                fluxHist[b].addLast(flux)
                if (fluxHist[b].size > histSize) fluxHist[b].removeFirst()

                val mean = fluxHist[b].average().toFloat()
                val variance = fluxHist[b].map { (it - mean).pow(2) }.average().toFloat()
                val sigma = sqrt(variance)
                val threshold = mean + 1.5f * sigma

                if (flux > threshold && flux > 0.005f) {
                    val normalised = ((flux - threshold) / (sigma + 0.001f)).coerceIn(0f, 3f)
                    map[channels[b]] = (normalised.toInt() + 1).coerceIn(1, 3)
                }
            }

            val hfEnergy = bandEnergyHz(fft, 6000f, 16000f)
            val hfFlux = (hfEnergy - prevHfEnergy).coerceAtLeast(0f)
            hfFluxHist.addLast(hfFlux)
            if (hfFluxHist.size > histSize) hfFluxHist.removeFirst()
            val hfMean = hfFluxHist.average().toFloat()
            val hfVar = hfFluxHist.map { (it - hfMean).pow(2) }.average().toFloat()
            val hfThresh = hfMean + 2.0f * sqrt(hfVar)
            if (hfFlux > hfThresh && hfFlux > 0.01f) map[channels[6]] =
                if (hfFlux > hfThresh * 1.5f) 3 else 2

            prevHfEnergy = hfEnergy
            prevFft = fft.copyOf()
            result += map
        }
        result
    }

    fun analyzeVolumeHeight(
        context: Context,
        uri: Uri,
        durationMs: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = mutableListOf<Map<Int, Int>>()
        val hannWindow = buildHannWindow(FFT_SIZE)

        var envelope = 0f
        val ATTACK = 0.95f
        val RELEASE = 0.70f
        var peakEnvelope = 0.01f
        val PEAK_DECAY = 0.995f

        var redEnvelope = 0f
        val RED_DECAY = 0.80f

        windows.forEach { window ->
            val fft = performFFT(window, hannWindow)

            var sumSq = 0f
            for (s in window) {
                val f = s.toFloat() / 32768f; sumSq += f * f
            }
            val rms = sqrt(sumSq / window.size.coerceAtLeast(1))

            envelope = if (rms > envelope) rms * ATTACK + envelope * (1f - ATTACK)
            else rms * (1f - RELEASE) + envelope * RELEASE

            peakEnvelope = maxOf(envelope, peakEnvelope * PEAK_DECAY).coerceAtLeast(0.005f)
            val normEnergy = (envelope / peakEnvelope).coerceIn(0f, 1f)

            val punchyEnergy = if (normEnergy > 0.1f) (normEnergy - 0.1f) / 0.9f else 0f
            val numLit = (punchyEnergy * 6.9f).toInt().coerceIn(0, 6)

            val map = mutableMapOf<Int, Int>()
            for (b in 0 until numLit) {
                map[channels[5 - b]] = 3
            }

            val subBass = bandEnergyHz(fft, 20f, 150f)
            redEnvelope = maxOf(subBass, redEnvelope * RED_DECAY)
            if (redEnvelope > 0.06f && normEnergy > 0.35f) {
                map[channels[6]] = 3
            }

            result += map
        }
        result
    }

    fun analyzeAdaptiveThreshold(
        context: Context,
        uri: Uri,
        durationMs: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = mutableListOf<Map<Int, Int>>()
        val hannWindow = buildHannWindow(FFT_SIZE)
        val histSize = 25
        val energyHist = Array(6) { ArrayDeque<Float>(histSize) }
        val prevEnergy = FloatArray(6)
        var prevSubFlux = 0f

        windows.forEach { window ->
            val fft = performFFT(window, hannWindow)
            val map = mutableMapOf<Int, Int>()

            for (b in 0..5) {
                val e = bandEnergy(fft, b)
                energyHist[b].addLast(e)
                if (energyHist[b].size > histSize) energyHist[b].removeFirst()

                val localMean = energyHist[b].average().toFloat()
                val localMax = energyHist[b].max()

                val relativeThresh = localMean + (localMax - localMean) * 0.55f
                val delta = e - prevEnergy[b]

                if (e > relativeThresh && delta > 0 && e > 0.02f) {
                    val rel =
                        ((e - relativeThresh) / (localMax - relativeThresh + 0.001f)).coerceIn(
                            0f,
                            1f
                        )
                    map[channels[b]] = when {
                        rel > 0.70f -> 3
                        rel > 0.35f -> 2
                        else -> 1
                    }
                }
                prevEnergy[b] = e
            }

            val sub = bandEnergyHz(fft, 20f, 120f)
            val flux = (sub - prevSubFlux).coerceAtLeast(0f)
            if (flux > 0.07f) map[channels[6]] = if (flux > 0.18f) 3 else 2
            prevSubFlux = sub

            result += map
        }
        result
    }

    fun analyzeMultiBand(
        context: Context,
        uri: Uri,
        durationMs: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = mutableListOf<Map<Int, Int>>()
        val hannWindow = buildHannWindow(FFT_SIZE)
        val smoothed = FloatArray(7)
        val peaks = FloatArray(7) { 0.05f }
        val ATTACK = 0.50f
        val RELEASE = 0.85f
        val P_DECAY = 0.9990f

        windows.forEach { window ->
            val fft = performFFT(window, hannWindow)
            val map = mutableMapOf<Int, Int>()

            for (b in 0..5) {
                val e = bandEnergy(fft, b)
                smoothed[b] = if (e > smoothed[b]) e * ATTACK + smoothed[b] * (1f - ATTACK)
                else e * (1f - RELEASE) + smoothed[b] * RELEASE
                peaks[b] = maxOf(smoothed[b], peaks[b] * P_DECAY).coerceAtLeast(0.001f)
                val norm = (smoothed[b] / peaks[b]).coerceIn(0f, 1f)

                val level = when {
                    norm > 0.70f -> 3
                    norm > 0.40f -> 2
                    norm > 0.15f -> 1
                    else -> 0
                }
                if (level > 0) map[channels[b]] = level
            }

            val sub = bandEnergyHz(fft, 20f, 150f)
            smoothed[6] = if (sub > smoothed[6]) sub * ATTACK + smoothed[6] * (1f - ATTACK)
            else sub * (1f - RELEASE) + smoothed[6] * RELEASE
            peaks[6] = maxOf(smoothed[6], peaks[6] * P_DECAY).coerceAtLeast(0.001f)
            val redNorm = (smoothed[6] / peaks[6]).coerceIn(0f, 1f)
            val redLevel = when {
                redNorm > 0.75f -> 3
                redNorm > 0.45f -> 2
                redNorm > 0.20f -> 1
                else -> 0
            }
            if (redLevel > 0) map[channels[6]] = redLevel

            result += map
        }
        result
    }

    fun analyzeBpmGrid(
        context: Context,
        uri: Uri,
        durationMs: Int,
        bpm: Int,
        channels: List<Int>
    ): List<Map<Int, Int>> = decodePcmWindows(context, uri) { windows ->
        val result = ArrayList<Map<Int, Int>>(windows.size)
        repeat(windows.size) { result.add(emptyMap()) }

        val hannWindow = buildHannWindow(FFT_SIZE)
        val msPerBeat = (60_000.0 / bpm).toLong()
        val winMs = 50L

        val onsetStrength = FloatArray(windows.size)
        var prevSubBass = 0f
        windows.forEachIndexed { i, w ->
            val fft = performFFT(w, hannWindow)
            val sub = bandEnergyHz(fft, 20f, 150f)
            onsetStrength[i] = (sub - prevSubBass).coerceAtLeast(0f)
            prevSubBass = sub
        }

        val SNAP_WINDOW_WINDOWS = 1
        var ts = 0L
        var beat = 0
        while (ts < durationMs - msPerBeat / 2) {
            val nominalWin = (ts / winMs).toInt().coerceIn(0, windows.size - 1)
            var bestWin = nominalWin
            var bestStrength = onsetStrength[nominalWin]
            for (delta in -SNAP_WINDOW_WINDOWS..SNAP_WINDOW_WINDOWS) {
                val w = (nominalWin + delta).coerceIn(0, windows.size - 1)
                if (onsetStrength[w] > bestStrength) {
                    bestStrength = onsetStrength[w]; bestWin = w
                }
            }

            val pattern: Map<Int, Int> = when (beat % 4) {
                0 -> channels.take(6).mapIndexed { i, ch -> ch to (3 - i / 2).coerceIn(1, 3) }
                    .toMap() +
                        mapOf(channels[6] to 3)

                2 -> mapOf(channels[2] to 3, channels[3] to 3, channels[4] to 2, channels[5] to 2)
                1, 3 -> mapOf(channels[0] to 2, channels[1] to 2)
                else -> emptyMap()
            }

            val spreadWins = ((msPerBeat * 0.40f) / winMs).toInt().coerceIn(1, 8)
            for (d in 0 until spreadWins) {
                val wi = (bestWin + d).coerceIn(0, windows.size - 1)
                result[wi] = pattern
            }

            ts += msPerBeat
            beat++
        }
        result
    }

    private fun <T> decodePcmWindows(
        context: Context,
        uri: Uri,
        process: (List<ShortArray>) -> T
    ): T {
        val windows = mutableListOf<ShortArray>()
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null

        try {
            extractor.setDataSource(context, uri, null)
            val trackIndex = selectAudioTrack(extractor)
            if (trackIndex >= 0) {
                extractor.selectTrack(trackIndex)
                val format = extractor.getTrackFormat(trackIndex)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
                val windowSizeSamples = (sampleRate * 0.050).toInt()

                if (mime.startsWith("audio/")) {
                    codec = MediaCodec.createDecoderByType(mime)
                    codec.configure(format, null, null, 0)
                    codec.start()

                    val info = MediaCodec.BufferInfo()
                    var extDone = false
                    var decDone = false
                    val pcm = java.util.ArrayDeque<Short>()

                    while (!decDone) {
                        if (!extDone) {
                            val inIdx = codec.dequeueInputBuffer(10_000)
                            if (inIdx >= 0) {
                                val buf = codec.getInputBuffer(inIdx)!!
                                val sz = extractor.readSampleData(buf, 0)
                                if (sz < 0) {
                                    codec.queueInputBuffer(
                                        inIdx,
                                        0,
                                        0,
                                        0,
                                        MediaCodec.BUFFER_FLAG_END_OF_STREAM
                                    )
                                    extDone = true
                                } else {
                                    codec.queueInputBuffer(inIdx, 0, sz, extractor.sampleTime, 0)
                                    extractor.advance()
                                }
                            }
                        }
                        val outIdx = codec.dequeueOutputBuffer(info, 10_000)
                        if (outIdx >= 0) {
                            codec.getOutputBuffer(outIdx)?.let { buf ->
                                while (buf.remaining() >= 2) pcm.add(buf.short)
                            }

                            while (pcm.size >= windowSizeSamples) {
                                val window = ShortArray(windowSizeSamples)
                                for (i in 0 until windowSizeSamples) window[i] = pcm.removeFirst()
                                windows.add(window)
                            }

                            codec.releaseOutputBuffer(outIdx, false)
                            if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) decDone =
                                true
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "PCM decode error: ${e.message}")
        } finally {
            codec?.stop(); codec?.release(); extractor.release()
        }

        @Suppress("UNCHECKED_CAST")
        return process(windows)
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            if (extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            ) return i
        }
        return -1
    }

    internal fun buildHannWindow(size: Int) = FloatArray(size) { i ->
        (0.5 * (1.0 - cos(2.0 * PI * i / (size - 1)))).toFloat()
    }

    internal fun performFFT(samples: ShortArray, window: FloatArray): FloatArray {
        val n = FFT_SIZE

        val offset = if (samples.size > n) (samples.size - n) / 2 else 0

        val re = FloatArray(n) { i ->
            if (i + offset < samples.size) (samples[i + offset].toFloat() / 32768f) * window[i] else 0f
        }
        val im = FloatArray(n)
        val m = log2(n.toDouble()).toInt()

        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val t = re[i]; re[i] = re[j]; re[j] = t
            }
            var k = n / 2
            while (k <= j) {
                j -= k; k /= 2
            }
            j += k
        }

        var le = 1
        repeat(m) {
            val le1 = le; le *= 2
            val ur = cos(PI / le1).toFloat()
            val ui = (-sin(PI / le1)).toFloat()
            var wr = 1f
            var wi = 0f
            for (j2 in 0 until le1) {
                var i = j2
                while (i < n) {
                    val ip = i + le1
                    val tr = re[ip] * wr - im[ip] * wi
                    val ti = re[ip] * wi + im[ip] * wr
                    re[ip] = re[i] - tr; im[ip] = im[i] - ti
                    re[i] += tr; im[i] += ti
                    i += le
                }
                val trr = wr * ur - wi * ui; wi = wr * ui + wi * ur; wr = trr
            }
        }

        return FloatArray(n / 2) { k -> sqrt(re[k] * re[k] + im[k] * im[k]) }
    }

    private fun bandEnergy(fft: FloatArray, b: Int): Float {
        val melMin = freqToMel(20f)
        val melMax = freqToMel(16000f)
        val step = (melMax - melMin) / 6f
        val loHz = melToFreq(melMin + b * step)
        val hiHz = melToFreq(melMin + (b + 1) * step)
        return bandEnergyHz(fft, loHz, hiHz)
    }

    private fun bandEnergyHz(fft: FloatArray, loHz: Float, hiHz: Float): Float {
        val lo = (loHz / HZ_PER_BIN).toInt().coerceIn(0, fft.size - 1)
        val hi = (hiHz / HZ_PER_BIN).toInt().coerceIn(0, fft.size - 1)
        if (lo > hi) return 0f
        var sum = 0f
        for (k in lo..hi) sum += fft[k]
        return sum / ((hi - lo + 1).coerceAtLeast(1))
    }

    private fun freqToMel(f: Float): Float = 2595f * log10(1f + f / 700f)
    private fun melToFreq(m: Float): Float = 700f * (10f.pow(m / 2595f) - 1f)

    private fun processWhiteLeds(
        magnitudes: FloatArray,
        currentMagnitudes: FloatArray,
        peaks: FloatArray,
        currentLevels: IntArray,
        channels: List<Int>
    ): Map<Int, Int> {
        val result = mutableMapOf<Int, Int>()
        for (i in 0..5) {
            val e = bandEnergy(magnitudes, i)
            peaks[i] = max(e, peaks[i] * PEAK_FALLOFF)
            val normalized = e / (peaks[i] + 0.001f)
            if (normalized > currentMagnitudes[i]) currentMagnitudes[i] = normalized
            else currentMagnitudes[i] *= RELEASE_RATE

            val mag = currentMagnitudes[i]
            val prev = currentLevels[i]
            val next = when (prev) {
                0 -> if (mag > 0.18f) 1 else 0
                1 -> when {
                    mag > 0.48f -> 2; mag < 0.10f -> 0; else -> 1
                }

                2 -> when {
                    mag > 0.78f -> 3; mag < 0.38f -> 1; else -> 2
                }

                3 -> if (mag < 0.68f) 2 else 3
                else -> 0
            }
            currentLevels[i] = next
            if (next > 0) result[channels[i]] = next
        }
        return result
    }

    private fun processRedOnset(magnitudes: FloatArray, prevMagnitudes: FloatArray?): Int {
        if (prevMagnitudes == null) return 0
        val binLo = (20f / HZ_PER_BIN).toInt()
        val binHi = (120f / HZ_PER_BIN).toInt()
        var flux = 0f
        for (i in binLo..binHi) {
            val d = magnitudes[i] - prevMagnitudes[i]; if (d > 0) flux += d
        }
        return if (flux > 0.15f) 3 else 0
    }
}
