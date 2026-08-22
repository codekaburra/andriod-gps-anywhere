package com.gpsanywhere.app

import com.gpsanywhere.app.service.speedBand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The band the varying walk speed wanders in.
 *
 * min/max are fixed when the walk starts; only the base speed moves afterwards.
 * The band must follow the base, or changing speed mid-route silently reverts.
 */
class SpeedBandTest {

    private fun kmh(v: Float) = v * 1000f / 3600f

    @Test
    fun `a base inside the bounds leaves them alone`() {
        val band = speedBand(base = kmh(4f), min = kmh(0f), max = kmh(20f))

        assertEquals(kmh(0f), band.start, 1e-4f)
        assertEquals(kmh(20f), band.endInclusive, 1e-4f)
    }

    @Test
    fun `a base above the ceiling raises it instead of being clamped down`() {
        // The reported bug: 300 km/h against the stale 20 km/h ceiling.
        val band = speedBand(base = kmh(300f), min = kmh(0f), max = kmh(20f))

        assertEquals(kmh(300f), band.endInclusive, 1e-4f)
        assertTrue(kmh(300f) in band)
    }

    @Test
    fun `a base below the floor lowers it`() {
        val band = speedBand(base = kmh(1f), min = kmh(5f), max = kmh(20f))

        assertEquals(kmh(1f), band.start, 1e-4f)
        assertTrue(kmh(1f) in band)
    }

    @Test
    fun `the base is always inside the band`() {
        val bounds = listOf(0f, 5f, 20f, 300f)
        for (base in listOf(0f, 4f, 16f, 20f, 21f, 150f, 300f)) {
            for (min in bounds) {
                for (max in bounds) {
                    if (min > max) continue
                    val band = speedBand(kmh(base), kmh(min), kmh(max))
                    assertTrue(
                        "base=$base min=$min max=$max produced $band",
                        kmh(base) in band
                    )
                }
            }
        }
    }

    @Test
    fun `inverted bounds still yield a usable range`() {
        // coerceIn(min, max) throws when min > max. Deriving both ends from the
        // base means lo <= base <= hi always holds, so this cannot blow up in a
        // coroutine where the crash would be invisible.
        val band = speedBand(base = kmh(50f), min = kmh(20f), max = kmh(10f))

        assertTrue(band.start <= band.endInclusive)
        assertTrue(kmh(50f) in band)
    }

    @Test
    fun `a speed set at the route maximum survives`() {
        val band = speedBand(base = kmh(300f), min = kmh(0f), max = kmh(300f))

        assertEquals(kmh(300f), band.endInclusive, 1e-4f)
    }
}
