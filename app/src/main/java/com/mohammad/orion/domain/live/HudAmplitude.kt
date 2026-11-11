package com.mohammad.orion.domain.live

/** Sanitizes normalized amplitude input; null means no available measurement, not audio activity. */
object HudAmplitude {
    fun level(amplitude: Float?, reducedMotion: Boolean): Float {
        if (reducedMotion || amplitude == null || !amplitude.isFinite()) return 0f
        return amplitude.coerceIn(0f, 1f)
    }
}
