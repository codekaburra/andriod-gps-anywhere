package com.gpsanywhere.app

import com.gpsanywhere.app.routes.LocationPoint
import com.gpsanywhere.app.util.parseClipboardCoordinates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseCoordinatesTest {

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `valid lat lng returns correct lat and lng`() {
        val result = parseClipboardCoordinates("25.0330, 121.5654")
        assertNotNull(result)
        assertEquals(25.0330, result!!.latitude, 0.0001)
        assertEquals(121.5654, result.longitude, 0.0001)
    }

    @Test
    fun `values without spaces around comma are parsed correctly`() {
        val result = parseClipboardCoordinates("25.0330,121.5654")
        assertNotNull(result)
        assertEquals(25.0330, result!!.latitude, 0.0001)
        assertEquals(121.5654, result.longitude, 0.0001)
    }

    @Test
    fun `negative latitude and longitude are parsed correctly`() {
        val result = parseClipboardCoordinates("-33.8688, 151.2093")
        assertNotNull(result)
        assertEquals(-33.8688, result!!.latitude, 0.0001)
        assertEquals(151.2093, result.longitude, 0.0001)
    }

    @Test
    fun `boundary values lat 90 lng 180 are valid`() {
        assertNotNull(parseClipboardCoordinates("90.0, 180.0"))
        assertNotNull(parseClipboardCoordinates("-90.0, -180.0"))
    }

    @Test
    fun `extra whitespace around values is trimmed`() {
        val result = parseClipboardCoordinates("  25.0330 ,  121.5654  ")
        assertNotNull(result)
        assertEquals(25.0330, result!!.latitude, 0.0001)
        assertEquals(121.5654, result.longitude, 0.0001)
    }

    // ── Out-of-range ──────────────────────────────────────────────────────────

    @Test
    fun `latitude above 90 returns null`() {
        assertNull(parseClipboardCoordinates("91.0, 121.5654"))
    }

    @Test
    fun `latitude below minus 90 returns null`() {
        assertNull(parseClipboardCoordinates("-91.0, 121.5654"))
    }

    @Test
    fun `longitude above 180 returns null`() {
        assertNull(parseClipboardCoordinates("25.0330, 181.0"))
    }

    @Test
    fun `longitude below minus 180 returns null`() {
        assertNull(parseClipboardCoordinates("25.0330, -181.0"))
    }

    // ── Malformed input ───────────────────────────────────────────────────────

    @Test
    fun `empty string returns null`() {
        assertNull(parseClipboardCoordinates(""))
    }

    @Test
    fun `single value without comma returns null`() {
        assertNull(parseClipboardCoordinates("25.0330"))
    }

    @Test
    fun `three values returns null`() {
        assertNull(parseClipboardCoordinates("25.0330, 121.5654, 0.0"))
    }

    @Test
    fun `non-numeric latitude returns null`() {
        assertNull(parseClipboardCoordinates("abc, 121.5654"))
    }

    @Test
    fun `non-numeric longitude returns null`() {
        assertNull(parseClipboardCoordinates("25.0330, xyz"))
    }

    @Test
    fun `both non-numeric returns null`() {
        assertNull(parseClipboardCoordinates("foo, bar"))
    }

    @Test
    fun `comma only returns null`() {
        assertNull(parseClipboardCoordinates(","))
    }

    // ── Ordering ──────────────────────────────────────────────────────────────

    @Test
    fun `latitude is the left value, longitude the right`() {
        // The invariant the route editor broke while this returned Pair(lng, lat):
        // 25.0330,121.5654 is Taipei, and reading it the other way round lands at
        // an impossible latitude of 121.
        val c = parseClipboardCoordinates("25.0330,121.5654")!!

        assertEquals(25.0330, c.latitude, 0.0001)
        assertEquals(121.5654, c.longitude, 0.0001)
        assertTrue(c.latitude in -90.0..90.0)
    }

    @Test
    fun `a parsed coordinate feeds a LocationPoint without swapping`() {
        val c = parseClipboardCoordinates("25.0330,121.5654")!!

        val point = LocationPoint(c.latitude, c.longitude)

        assertEquals(25.0330, point.latitude, 0.0001)
        assertEquals(121.5654, point.longitude, 0.0001)
    }
}
