package com.example.maya

import kotlin.math.*

/**
 * AudioPreprocessor
 * - Converts PCM float audio (mono, 16 kHz) to log-mel spectrogram suitable for speaker embedding models.
 * - Parameters are configurable; adjust to match your model's expected input.
 */
object AudioPreprocessor {
    fun floatToMelSpectrogram(samples: FloatArray, sampleRate: Int = 16000, nFft: Int = 512, hop: Int = 160, nMels: Int = 40): Array<FloatArray> {
        // 1. Frame signal
        val frames = enframe(samples, nFft, hop)
        // 2. Window and compute power spectrum
        val window = hamming(nFft)
        val mag = Array(frames.size) { FloatArray(nFft / 2 + 1) }
        for (i in frames.indices) {
            val frame = frames[i]
            val win = FloatArray(nFft)
            for (j in frame.indices) win[j] = frame[j] * window[j]
            val spectrum = rfftMagnitude(win)
            mag[i] = spectrum
        }
        // 3. Apply mel filterbank
        val melFilter = melFilterBank(nFft, sampleRate, nMels)
        val melSpec = Array(mag.size) { FloatArray(nMels) }
        for (i in mag.indices) {
            for (m in 0 until nMels) {
                var s = 0f
                for (k in mag[i].indices) s += mag[i][k] * melFilter[m][k]
                melSpec[i][m] = ln(1e-6f + max(0f, s))
            }
        }
        return melSpec
    }

    private fun enframe(signal: FloatArray, frameLen: Int, hop: Int): Array<FloatArray> {
        val frames = mutableListOf<FloatArray>()
        var start = 0
        while (start + frameLen <= signal.size) {
            frames.add(signal.copyOfRange(start, start + frameLen))
            start += hop
        }
        return frames.toTypedArray()
    }

    private fun hamming(n: Int): FloatArray {
        val out = FloatArray(n)
        for (i in 0 until n) out[i] = (0.54 - 0.46 * cos(2.0 * Math.PI * i / (n - 1))).toFloat()
        return out
    }

    // Placeholder FFT magnitude using naive DFT (inefficient). For production use a native FFT library.
    private fun rfftMagnitude(frame: FloatArray): FloatArray {
        val n = frame.size
        val half = n / 2
        val out = FloatArray(half + 1)
        for (k in 0..half) {
            var re = 0.0
            var im = 0.0
            for (t in 0 until n) {
                val angle = 2.0 * Math.PI * k * t / n
                re += frame[t] * cos(angle)
                im -= frame[t] * sin(angle)
            }
            out[k] = (re * re + im * im).toFloat()
        }
        return out
    }

    private fun melFilterBank(nFft: Int, sampleRate: Int, nMels: Int): Array<FloatArray> {
        val fmin = 0.0
        val fmax = sampleRate / 2.0
        fun hzToMel(hz: Double) = 2595.0 * ln10(hz / 700.0 + 1.0)
        fun melToHz(mel: Double) = 700.0 * (10.0.pow(mel / 2595.0) - 1.0)
        val melMin = hzToMel(fmin)
        val melMax = hzToMel(fmax)
        val mels = DoubleArray(nMels + 2) { i -> melMin + (melMax - melMin) * i / (nMels + 1) }
        val hz = DoubleArray(mels.size) { i -> melToHz(mels[i]) }
        val bins = hz.map { floor((nFft + 1) * it / sampleRate).toInt() }
        val filters = Array(nMels) { FloatArray(nFft / 2 + 1) }
        for (m in 1..nMels) {
            val f_m_minus = bins[m - 1]
            val f_m = bins[m]
            val f_m_plus = bins[m + 1]
            for (k in 0 until (nFft / 2 + 1)) {
                val v = when {
                    k < f_m_minus -> 0.0
                    k <= f_m -> (k - f_m_minus).toDouble() / (f_m - f_m_minus)
                    k <= f_m_plus -> (f_m_plus - k).toDouble() / (f_m_plus - f_m)
                    else -> 0.0
                }
                filters[m - 1][k] = v.toFloat()
            }
        }
        return filters
    }

    private fun ln10(x: Double): Double = ln(x)
}
