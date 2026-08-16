package com.gpsanywhere.app.data

/**
 * Splits one CSV line into fields, honouring double-quoted fields that may
 * contain commas, with `""` as an escaped quote inside them.
 *
 * Shared by the bundled location packs, the bundled route packs and the
 * user-facing CSV import. They all have to agree on where a field ends, and a
 * place name with a comma in it is common enough that each of them needs the
 * quoting rule rather than a plain split(',').
 */
internal fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val ch = line[i]
        when {
            ch == '"' && !inQuotes -> inQuotes = true
            ch == '"' && inQuotes -> {
                if (i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ } // escaped ""
                else inQuotes = false
            }
            ch == ',' && !inQuotes -> { fields.add(sb.toString()); sb.clear() }
            else -> sb.append(ch)
        }
        i++
    }
    fields.add(sb.toString())
    return fields
}
