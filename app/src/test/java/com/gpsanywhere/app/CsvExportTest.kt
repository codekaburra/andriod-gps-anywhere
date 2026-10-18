package com.gpsanywhere.app

import com.gpsanywhere.app.data.CsvExport
import com.gpsanywhere.app.data.DefaultSavedRouteSeeder
import com.gpsanywhere.app.data.LocationCsvImport
import com.gpsanywhere.app.data.SavedLocation
import com.gpsanywhere.app.data.SavedRoute
import com.gpsanywhere.app.data.WaypointJson
import com.gpsanywhere.app.routes.LocationPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CsvExportTest {

    @Test
    fun `exported locations import back unchanged`() {
        val saved = listOf(
            SavedLocation(name = "香港藝術館", nameEn = "Hong Kong Museum of Art", latitude = 22.2937758, longitude = 114.1721957, tags = "香港", tagsEn = "Hong Kong"),
            SavedLocation(name = "", nameEn = "Shop, \"The\" Corner", latitude = -33.8688, longitude = 151.2093),
            SavedLocation(name = "金閣寺", latitude = 35.0394, longitude = 135.7292)
        )

        val result = LocationCsvImport.parse(CsvExport.locations(saved))

        assertTrue(result.problems.isEmpty())
        assertEquals(saved.size, result.rows.size)
        saved.zip(result.rows).forEach { (expected, row) ->
            assertEquals(expected.name, row.name)
            assertEquals(expected.nameEn, row.nameEn)
            assertEquals(expected.latitude, row.latitude, 1e-6)
            assertEquals(expected.longitude, row.longitude, 1e-6)
            assertEquals(expected.tags, row.tags)
            assertEquals(expected.tagsEn, row.tagsEn)
        }
    }

    @Test
    fun `no locations exports only the header`() {
        assertEquals(LocationCsvImport.COLUMNS + "\n", CsvExport.locations(emptyList()))
    }

    @Test
    fun `each exported route block parses back as that route`() {
        val first = listOf(LocationPoint(22.2943, 114.1699, "鐘樓, 尖沙咀"), LocationPoint(22.2937, 114.1732, null))
        val second = listOf(LocationPoint(37.5665, 126.9780, "Seoul"), LocationPoint(37.5700, 126.9800, "City Hall"))
        val routes = listOf(
            route(name = "Tsim Sha Tsui", nameTc = "尖沙咀", points = first),
            route(name = "Seoul", nameTc = "", points = second)
        )

        val blocks = CsvExport.routes(routes).split("\n\n")

        assertEquals(2, blocks.size)
        routes.zip(blocks).forEach { (expected, block) ->
            val parsed = DefaultSavedRouteSeeder.parseCsv(block)!!
            assertEquals(expected.name, parsed.routeName)
            val points = WaypointJson.fromJson(expected.waypointsJson)
            assertEquals(points.size, parsed.coordinates.size)
            points.zip(parsed.coordinates).forEach { (p, c) ->
                assertEquals(p.latitude, c.latitude, 1e-6)
                assertEquals(p.longitude, c.longitude, 1e-6)
                assertEquals(p.name.orEmpty(), c.name)
            }
        }
        assertEquals("尖沙咀", DefaultSavedRouteSeeder.parseCsv(blocks[0])!!.routeNameTc)
    }

    @Test
    fun `coordinates use a dot whatever the default locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val csv = CsvExport.locations(listOf(SavedLocation(name = "A", latitude = 1.5, longitude = 2.25)))
            assertEquals(1, LocationCsvImport.parse(csv).rows.size)
        } finally {
            Locale.setDefault(original)
        }
    }

    private fun route(name: String, nameTc: String, points: List<LocationPoint>) = SavedRoute(
        name = name,
        nameTc = nameTc,
        waypointsJson = WaypointJson.toJson(points),
        routeMethod = "straight"
    )
}
