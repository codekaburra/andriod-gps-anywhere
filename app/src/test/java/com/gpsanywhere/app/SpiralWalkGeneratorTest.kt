package com.gpsanywhere.app

import com.gpsanywhere.app.routes.SpiralWalkGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SpiralWalkGeneratorTest {

    @Test
    fun `lats and lngs have equal length`() {
        val (lats, lngs) = SpiralWalkGenerator.generate(22.3, 114.0, rings = 5)
        assertEquals(lats.size, lngs.size)
    }

    @Test
    fun `first point is the centre`() {
        val (lats, lngs) = SpiralWalkGenerator.generate(22.3166, 114.0453, rings = 3)
        assertEquals(22.3166, lats.first(), 1e-9)
        assertEquals(114.0453, lngs.first(), 1e-9)
    }

    @Test
    fun `point count matches the ring formula`() {
        // 1 (centre) + 2 * rings * (2 * rings + 1)
        val rings = 15
        val expected = 1 + 2 * rings * (2 * rings + 1)
        val (lats, _) = SpiralWalkGenerator.generate(0.0, 0.0, rings = rings)
        assertEquals(expected, lats.size)
    }

    @Test
    fun `consecutive points are exactly one step apart`() {
        val step = 0.0003
        val (lats, lngs) = SpiralWalkGenerator.generate(1.0, 2.0, stepDeg = step, rings = 4)
        for (i in 1 until lats.size) {
            val d = abs(lats[i] - lats[i - 1]) + abs(lngs[i] - lngs[i - 1])
            assertEquals("step $i", step, d, 1e-9)
        }
    }

    @Test
    fun `first move is one step east`() {
        val step = 0.0002
        val (lats, lngs) = SpiralWalkGenerator.generate(10.0, 20.0, stepDeg = step, rings = 2)
        assertEquals(10.0, lats[1], 1e-9)          // latitude unchanged
        assertTrue(lngs[1] > lngs[0])              // longitude increased (east)
        assertEquals(20.0 + step, lngs[1], 1e-9)
    }
}
