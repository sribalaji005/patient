package com.example.iot

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Realistic synthetic ECG (Electrocardiogram) waveform generator
 * based on standard P-Q-R-S-T cardiac cycle equations.
 */
object EcgWaveGenerator {

    /**
     * Computes the normalized ECG signal value at a given phase in the cardiac cycle [0.0, 1.0).
     * Returns a float roughly between -0.4 and 1.0.
     */
    fun getSample(phase: Float): Float {
        val p = phase % 1.0f

        // Baseline wandering / slight biological noise
        val baseline = 0.02f * sin(2.0 * PI * p).toFloat()

        // P-Wave (Atrial depolarization): ~0.15 - 0.25
        val pWave = if (p in 0.12f..0.22f) {
            0.15f * exp(-((p - 0.17f) * (p - 0.17f)) / 0.0006f)
        } else 0f

        // Q-Wave (Septal depolarization): small negative dip at ~0.35
        val qWave = if (p in 0.33f..0.37f) {
            -0.12f * exp(-((p - 0.35f) * (p - 0.35f)) / 0.00015f)
        } else 0f

        // R-Wave (Ventricular depolarization spike): sharp tall positive spike at ~0.39
        val rWave = if (p in 0.36f..0.43f) {
            1.0f * exp(-((p - 0.39f) * (p - 0.39f)) / 0.00025f)
        } else 0f

        // S-Wave (Ventricular depolarization): sharp negative dip at ~0.42
        val sWave = if (p in 0.40f..0.46f) {
            -0.28f * exp(-((p - 0.43f) * (p - 0.43f)) / 0.0002f)
        } else 0f

        // T-Wave (Ventricular repolarization): rounded positive deflection at ~0.65
        val tWave = if (p in 0.55f..0.75f) {
            0.26f * exp(-((p - 0.65f) * (p - 0.65f)) / 0.0035f)
        } else 0f

        return baseline + pWave + qWave + rWave + sWave + tWave
    }
}
