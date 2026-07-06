package com.gpsanywhere.app

import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RouteCsvParseTest {

    @Test
    fun `parses eng and tc names plus leading id`() {
        val csv = """
            # hk_disneyland_resort
            # route_name_tc: 香港-迪士尼樂園
            # route_name_eng: Hong Kong Disneyland
            latitude,longitude,name_tc,name_en
            22.313500,114.039800,正門,Main Entrance
            22.314000,114.040000,廣場,Plaza
        """.trimIndent()

        val route = DefaultSavedRouteSeeder.parseCsv(csv)!!
        assertEquals("Hong Kong Disneyland", route.routeName)
        assertEquals("hk_disneyland_resort", route.routeId)
        assertEquals(2, route.coordinates.size)
        assertEquals(22.3135, route.coordinates[0].latitude, 1e-6)
        assertEquals("正門", route.coordinates[0].name)   // waypoint name comes from name_tc
    }

    @Test
    fun `falls back to tc name when eng missing`() {
        val csv = """
            # some_id
            # route_name_tc: 只有中文
            latitude,longitude,name_tc,name_en
            22.0,114.0,甲,A
        """.trimIndent()
        assertEquals("只有中文", DefaultSavedRouteSeeder.parseCsv(csv)!!.routeName)
    }

    @Test
    fun `supports the legacy route_name header`() {
        val csv = """
            # route_name: Legacy Route
            latitude,longitude,name
            22.0,114.0,A
            22.1,114.1,B
        """.trimIndent()
        val route = DefaultSavedRouteSeeder.parseCsv(csv)!!
        assertEquals("Legacy Route", route.routeName)
        assertEquals(2, route.coordinates.size)
    }

    @Test
    fun `returns null when no name or no coordinates`() {
        assertNull(DefaultSavedRouteSeeder.parseCsv("# route_name_eng: Empty\nlatitude,longitude,name_tc,name_en"))
        assertNull(DefaultSavedRouteSeeder.parseCsv("22.0,114.0,A,A"))
    }
}
