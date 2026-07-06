package com.gpsanywhere.app

import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.routes.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WaypointJsonTest {

    @Test
    fun `round trips a list of waypoints`() {
        val points = listOf(
            LocationPoint(22.3168, 114.0451, "A"),
            LocationPoint(-33.8688, 151.2093, null),
            LocationPoint(0.0, 0.0, "origin")
        )
        val restored = WaypointJson.fromJson(WaypointJson.toJson(points))
        assertEquals(points, restored)
    }

    @Test
    fun `blank json returns empty list`() {
        assertTrue(WaypointJson.fromJson("").isEmpty())
        assertTrue(WaypointJson.fromJson("   ").isEmpty())
    }

    @Test
    fun `empty list round trips`() {
        val restored = WaypointJson.fromJson(WaypointJson.toJson(emptyList()))
        assertTrue(restored.isEmpty())
    }
}
