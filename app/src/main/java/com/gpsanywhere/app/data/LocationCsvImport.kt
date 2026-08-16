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
            if (!seenData && isHeaderRow(parts)) {
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

    /**
     * True when the first line is column names rather than a location.
     *
     * Both of the first two fields have to be non-numeric. Testing only the
     * first one looks equivalent and is not: a real row whose latitude has a
     * typo — `abc,114.18,銅鑼灣` — then reads as a header and disappears without
     * a word, which is the silent drop this parser exists to avoid. Its
     * longitude is still a number, so requiring both keeps it a reported row.
     *
     * Language-agnostic on purpose: `緯度,經度,名稱` is as much a header as
     * `latitude,longitude,name_tc`. A first line that is non-numeric in both
     * columns and *not* a header is unrecoverably ambiguous, and dropping it is
     * the better of the two guesses.
     */
    private fun isHeaderRow(parts: List<String>): Boolean =
        parts.getOrNull(0)?.toDoubleOrNull() == null &&
            parts.getOrNull(1)?.toDoubleOrNull() == null

    /**
     * How close two positions must be to count as the same place: about a metre.
     */
    const val DUPLICATE_EPSILON = 1e-5

    /**
     * True when [existing] is already this row's location.
     *
     * Position has to match, and so does at least one of the two names — a
     * blank name is not evidence of anything, so it never counts as a match.
     * Comparing only the Chinese name would fuse two different English-only
     * locations that happen to share coordinates, since both carry `name = ""`.
     */
    fun Row.isSameAs(existing: SavedLocation): Boolean {
        if (kotlin.math.abs(existing.latitude - latitude) >= DUPLICATE_EPSILON) return false
        if (kotlin.math.abs(existing.longitude - longitude) >= DUPLICATE_EPSILON) return false
        val tcMatches = name.isNotBlank() && existing.name.trim().equals(name.trim(), ignoreCase = true)
        val enMatches = nameEn.isNotBlank() && existing.nameEn.trim().equals(nameEn.trim(), ignoreCase = true)
        return tcMatches || enMatches
    }
}
