package com.example.maya

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

// Pure-Kotlin audio feature helpers (no Android APIs) so they can be unit-tested.

fun preprocessAudio(input: FloatArray): FloatArray {
    if (input.isEmpty()) return input
    val out = FloatArray(input.size)
    var mean = 0f
    for (v in input) mean += v
    mean /= input.size
    for (i in input.indices) out[i] = input[i] - mean
    val preEmph = 0.97f
    var prev = out[0]
    for (i in 1 until out.size) {
        val cur = out[i]
        out[i] = cur - preEmph * prev
        prev = cur
    }
    var sumSq = 0.0
    for (v in out) sumSq += (v * v)
    val rms = sqrt((sumSq / out.size).toDouble()).toFloat()
    val target = 0.1f
    if (rms > 0f) {
        val scale = target / rms
        for (i in out.indices) out[i] = out[i] * scale
    }
    return out
}

fun computeLogMelSpectrogram(audio: FloatArray, sampleRate: Int = 16000, melBins: Int = 40): FloatArray {
    if (audio.isEmpty()) return FloatArray(melBins) { 0f }
    val frameLen = (0.025 * sampleRate).toInt()
    val frameShift = (0.010 * sampleRate).toInt()
    val nfft = 512
    val win = FloatArray(frameLen) { i -> (0.5f - 0.5f * cos(2.0 * Math.PI * i / frameLen)).toFloat() }
    val frames = mutableListOf<FloatArray>()
    var i = 0
    while (i + frameLen <= audio.size) {
        val frame = FloatArray(nfft)
        for (j in 0 until frameLen) frame[j] = audio[i + j] * win[j]
        frames.add(frame)
        i += frameShift
    }
    if (frames.isEmpty()) frames.add(FloatArray(nfft))
    val powerSpecs = Array(frames.size) { FloatArray(nfft / 2 + 1) }
    for (fi in frames.indices) powerSpecs[fi] = powerSpectrum(frames[fi], nfft)
    val melFilters = melFilterBank(melBins, nfft, sampleRate)
    val melEnergies = Array(powerSpecs.size) { FloatArray(melBins) }
    for (fi in powerSpecs.indices) {
        val spec = powerSpecs[fi]
        for (m in 0 until melBins) {
            var sum = 0.0f
            val filt = melFilters[m]
            for (k in spec.indices) sum += spec[k] * filt[k]
            melEnergies[fi][m] = kotlin.math.log10(kotlin.math.max(1e-10f, sum))
        }
    }
    val avg = FloatArray(melBins)
    for (m in 0 until melBins) {
        var s = 0f
        for (t in melEnergies.indices) s += melEnergies[t][m]
        avg[m] = s / melEnergies.size
    }
    return avg
}

// Return per-frame log-mel spectrogram as array of frames (time) x melBins (freq)
fun computeLogMelSpectrogramFrames(audio: FloatArray, sampleRate: Int = 16000, melBins: Int = 40): Array<FloatArray> {
    if (audio.isEmpty()) return arrayOf()
    val frameLen = (0.025 * sampleRate).toInt()
    val frameShift = (0.010 * sampleRate).toInt()
    val nfft = 512
    val win = FloatArray(frameLen) { i -> (0.5f - 0.5f * cos(2.0 * Math.PI * i / frameLen)).toFloat() }
    val frames = mutableListOf<FloatArray>()
    var i = 0
    while (i + frameLen <= audio.size) {
        val frame = FloatArray(nfft)
        for (j in 0 until frameLen) frame[j] = audio[i + j] * win[j]
        frames.add(frame)
        i += frameShift
    }
    if (frames.isEmpty()) frames.add(FloatArray(nfft))
    val powerSpecs = Array(frames.size) { FloatArray(nfft / 2 + 1) }
    for (fi in frames.indices) powerSpecs[fi] = powerSpectrum(frames[fi], nfft)
    val melFilters = melFilterBank(melBins, nfft, sampleRate)
    val melEnergies = Array(powerSpecs.size) { FloatArray(melBins) }
    for (fi in powerSpecs.indices) {
        val spec = powerSpecs[fi]
        for (m in 0 until melBins) {
            var sum = 0.0f
            val filt = melFilters[m]
            for (k in spec.indices) sum += spec[k] * filt[k]
            melEnergies[fi][m] = kotlin.math.log10(kotlin.math.max(1e-10f, sum))
        }
    }
    return melEnergies
}

private fun powerSpectrum(frame: FloatArray, nfft: Int): FloatArray {
    val half = nfft / 2
    val out = FloatArray(half + 1)
    for (k in 0..half) {
        var re = 0.0
        var im = 0.0
        for (n in frame.indices) {
            val angle = -2.0 * Math.PI * k * n / nfft
            re += frame[n] * cos(angle).toFloat()
            im += frame[n] * sin(angle).toFloat()
        }
        val mag = (re * re + im * im).toFloat()
        out[k] = mag
    }
    return out
}

private fun melFilterBank(nFilters: Int, nfft: Int, sampleRate: Int): Array<FloatArray> {
    val fMin = 0.0
    val fMax = sampleRate / 2.0
    fun hzToMel(f: Double) = 2595.0 * kotlin.math.log10(1.0 + f / 700.0)
    fun melToHz(m: Double) = 700.0 * (10.0.pow(m / 2595.0) - 1.0)
    val melMin = hzToMel(fMin)
    val melMax = hzToMel(fMax)
    val melPoints = DoubleArray(nFilters + 2) { i -> melMin + (melMax - melMin) * i / (nFilters + 1) }
    val hzPoints = DoubleArray(melPoints.size) { i -> melToHz(melPoints[i]) }
    val bin = IntArray(hzPoints.size) { i -> floor((nfft + 1) * hzPoints[i] / sampleRate).toInt().coerceAtMost(nfft / 2) }
    val filters = Array(nFilters) { FloatArray(nfft / 2 + 1) }
    for (m in 1..nFilters) {
        val f_m_minus = bin[m - 1]
        val f_m = bin[m]
        val f_m_plus = bin[m + 1]
        for (k in 0..nfft / 2) {
            filters[m - 1][k] = when {
                k < f_m_minus -> 0f
                k <= f_m -> ((k - f_m_minus).toDouble() / (f_m - f_m_minus)).toFloat()
                k <= f_m_plus -> ((f_m_plus - k).toDouble() / (f_m_plus - f_m)).toFloat()
                else -> 0f
            }
        }
    }
    return filters
}

fun melTo128(mel: FloatArray): FloatArray {
    val out = FloatArray(128)
    val m = mel.size
    for (i in 0 until 128) {
        if (i < m) out[i] = mel[i] else out[i] = mel[i % m]
    }
    var norm = 0f
    for (v in out) norm += v * v
    norm = sqrt(norm)
    if (norm > 0f) for (i in out.indices) out[i] = out[i] / norm
    return out
}

fun FloatArrayToByteArray(f: FloatArray): ByteArray {
    val bb = ByteBuffer.allocate(f.size * 4).order(ByteOrder.LITTLE_ENDIAN)
    for (v in f) bb.putFloat(v)
    return bb.array()
}

fun ByteArrayToFloatArray(b: ByteArray): FloatArray {
    val fb = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
    val fa = FloatArray(b.size / 4)
    for (i in fa.indices) fa[i] = fb.float
    return fa
}
