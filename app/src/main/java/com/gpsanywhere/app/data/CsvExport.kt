package com.gpsanywhere.app.data

import java.util.Locale

/**
 * Writes saved data as CSV in the formats the app already imports, so an export
 * pasted back in recreates what was saved.
 */
object CsvExport {

    /** One row per location, in [LocationCsvImport.COLUMNS] order. */
    fun locations(locations: List<SavedLocation>): String = buildString {
        appendLine(LocationCsvImport.COLUMNS)
        locations.forEach { location ->
            appendLine(
                listOf(
                    coordinate(location.latitude),
                    coordinate(location.longitude),
                    csvEscape(location.name),
                    csvEscape(location.nameEn),
                    csvEscape(location.tags),
                    csvEscape(location.tagsEn)
                ).joinToString(",")
            )
        }
    }

    /**
     * One block per route, separated by a blank line. Each block is the route
     * editor's CSV format, which holds a single route, so blocks are pasted back
     * one at a time.
     */
    fun routes(routes: List<SavedRoute>): String = routes.joinToString("\n") { route ->
        buildString {
            appendLine("# route_name: ${route.name.trim()}")
            if (route.nameTc.isNotBlank()) appendLine("# route_name_tc: ${route.nameTc.trim()}")
            appendLine("latitude,longitude,name_tc,name_en")
            WaypointJson.fromJson(route.waypointsJson).forEach { point ->
                appendLine("${coordinate(point.latitude)},${coordinate(point.longitude)},${csvEscape(point.name.orEmpty())},")
            }
        }
    }

    // Locale.US: a comma decimal separator would split the coordinate into two fields.
    private fun coordinate(value: Double): String = "%.6f".format(Locale.US, value)
}
