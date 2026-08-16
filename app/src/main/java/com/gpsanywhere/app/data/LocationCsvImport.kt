package com.gpsanywhere.app.data

/**
 * Parses the CSV a user pastes into the bulk import screen.
 *
 * Deliberately not [DefaultLocationSeeder.parseCsv], which serves the bundled
 * packs and differs in three ways that matter here:
 *
 *  - it requires a `# pack_name:` header and returns null without one,
 *  - it derives a `sourceId`, marking rows as preinstalled,
 *  - it drops malformed rows with `continue`, saying nothing.
 *
 * Someone pasting twenty lines needs to be told which line was wrong, so this
 * reports every rejected row instead of silently importing fewer than expected.
 */
object LocationCsvImport {

    /** Columns, in order. Only the first three are required. */
    const val COLUMNS = "latitude,longitude,name_tc,name_en,tags_tc,tags_en"

    /**
     * Sample content for the "fill in example" button.
     *
     * Two rows lifted verbatim from the bundled packs (assets/saved_locations),
     * so the example is a real pair of coordinates rather than invented ones and
     * shows every column filled in, including both tag languages.
     *
     * Data rows only. [COLUMNS] is already printed above the field, so repeating
     * it inside would just be a line the user has to recognise as not-a-location
     * before deleting it.
     *
     * Not a string resource: the row already carries its Chinese and English
     * names side by side, so there is nothing here for a translator to change.
     */
    val EXAMPLE = """
        22.2937758,114.1721957,香港藝術館,Hong Kong Museum of Art,香港,Hong Kong
        35.0394,135.7292,金閣寺,Kinkaku-ji,日本,Japan
    """.trimIndent()

    /** A row that parsed and validated. Line numbers are 1-based, as shown to the user. */
    data class Row(
        val lineNumber: Int,
        val name: String,
        val nameEn: String,
        val latitude: Double,
        val longitude: Double,
        val tags: String,
        val tagsEn: String
    )

    /** Why a row was rejected. The screen maps these onto localised strings. */
    enum class Reason { TOO_FEW_COLUMNS, BAD_LATITUDE, BAD_LONGITUDE, NO_NAME }

    data class Problem(val lineNumber: Int, val text: String, val reason: Reason)

    data class Result(val rows: List<Row>, val problems: List<Problem>)

    /**
     * Splits [content] into accepted rows and rejected ones.
     *
     * Blank lines and `#` comments are ignored, so a bundled pack file pasted in
     * whole works without the user stripping its header first. A leading header
     * row is detected rather than assumed: it is skipped only when its first
     * field is not a number, so a paste that starts straight at the data does
     * not lose its first location.
     */
    fun parse(content: String): Result {
        val rows = mutableListOf<Row>()
        val problems = mutableListOf<Problem>()
        var seenData = false

        content.lines().forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed

            val lineNumber = index + 1
            val parts = parseCsvLine(line).map { it.trim() }

            // A header row only counts as one before any data has been read;
            // later junk is a broken row and should be reported as such.
            if (!seenData && parts.firstOrNull()?.toDoubleOrNull() == null) {
                seenData = true
                return@forEachIndexed
            }
            seenData = true

            if (parts.size < 3) {
                problems += Problem(lineNumber, line, Reason.TOO_FEW_COLUMNS)
                return@forEachIndexed
            }

            val lat = parts[0].toDoubleOrNull()
            if (lat == null || lat !in -90.0..90.0) {
                problems += Problem(lineNumber, line, Reason.BAD_LATITUDE)
                return@forEachIndexed
            }

            val lng = parts[1].toDoubleOrNull()
            if (lng == null || lng !in -180.0..180.0) {
                problems += Problem(lineNumber, line, Reason.BAD_LONGITUDE)
                return@forEachIndexed
            }

            val nameTc = parts[2]
            val nameEn = parts.getOrElse(3) { "" }
            // Same rule as the single-location editor: one of the two names is
            // enough, because displayName() falls back to whichever is present.
            if (nameTc.isBlank() && nameEn.isBlank()) {
                problems += Problem(lineNumber, line, Reason.NO_NAME)
                return@forEachIndexed
            }

            rows += Row(
                lineNumber = lineNumber,
                name = nameTc,
                nameEn = nameEn,
                latitude = lat,
                longitude = lng,
                tags = parts.getOrElse(4) { "" },
                tagsEn = parts.getOrElse(5) { "" }
            )
        }

        return Result(rows, problems)
    }
}
