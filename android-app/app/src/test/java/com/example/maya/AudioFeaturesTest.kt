package com.example.maya

import org.junit.Assert.*
import org.junit.Test

class AudioFeaturesTest {
    @Test
    fun floatByteRoundtrip() {
        val src = floatArrayOf(0.1f, -0.2f, 0.3f, 0.0f)
        val b = FloatArrayToByteArray(src)
        val out = ByteArrayToFloatArray(b)
        assertArrayEquals(src, out, 1e-6f)
    }

    @Test
    fun melTo128ProducesLength128() {
        val mel = FloatArray(40) { i -> i.toFloat() }
        val emb = melTo128(mel)
        assertEquals(128, emb.size)
        // normalized
        var sumSq = 0f
        for (v in emb) sumSq += v * v
        assertTrue(sumSq > 0f)
    }

    @Test
    fun computeLogMelReturnsCorrectSize() {
        val samples = FloatArray(16000) { 0.01f * Math.sin(2.0 * Math.PI * 440.0 * it / 16000.0).toFloat() }
        val mel = computeLogMelSpectrogram(samples, 16000, 40)
        assertEquals(40, mel.size)
    }

    @Test
    fun preprocessNormalizes() {
        val samples = FloatArray(1000) { if (it % 2 == 0) 0.5f else -0.5f }
        val p = preprocessAudio(samples)
        var sumSq = 0.0
        for (v in p) sumSq += (v * v)
        val rms = Math.sqrt(sumSq / p.size)
        // target ~0.1
        assertTrue(rms > 0.05 && rms < 0.2)
    }
}
