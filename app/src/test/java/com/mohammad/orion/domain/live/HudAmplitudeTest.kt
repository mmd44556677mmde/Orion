package com.mohammad.orion.domain.live

import org.junit.Assert.assertEquals
import org.junit.Test

class HudAmplitudeTest {
    @Test fun handlesUnavailableInvalidAndOutOfRangeInputs() {
        assertEquals(0f, HudAmplitude.level(null, false), 0f)
        assertEquals(0f, HudAmplitude.level(Float.NaN, false), 0f)
        assertEquals(0f, HudAmplitude.level(Float.POSITIVE_INFINITY, false), 0f)
        assertEquals(0f, HudAmplitude.level(-1f, false), 0f)
        assertEquals(1f, HudAmplitude.level(2f, false), 0f)
        assertEquals(0.5f, HudAmplitude.level(0.5f, false), 0f)
    }
    @Test fun reducedMotionDoesNotAnimate() {
        assertEquals(0f, HudAmplitude.level(1f, true), 0f)
        assertEquals(0f, HudAmplitude.level(null, true), 0f)
    }
}
