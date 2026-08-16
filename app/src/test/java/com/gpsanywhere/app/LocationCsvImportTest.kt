package com.gpsanywhere.app

import com.gpsanywhere.app.data.LocationCsvImport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationCsvImportTest {

    @Test
    fun `parses rows with a header present`() {
        val result = LocationCsvImport.parse(
            """
            latitude,longitude,name_tc,name_en,tags_tc,tags_en
            22.2800,114.1800,銅鑼灣,Causeway Bay,購物,shopping
            22.3193,114.1694,旺角,Mong Kok,,
            """.trimIndent()
        )

        assertTrue(result.problems.isEmpty())
        assertEquals(2, result.rows.size)
        assertEquals("銅鑼灣", result.rows[0].name)
        assertEquals("Causeway Bay", result.rows[0].nameEn)
        assertEquals(22.28, result.rows[0].latitude, 1e-9)
        assertEquals("shopping", result.rows[0].tagsEn)
        assertEquals("", result.rows[1].tags)
    }

    @Test
    fun `first row is kept when there is no header`() {
        val result = LocationCsvImport.parse("22.28,114.18,銅鑼灣,Causeway Bay")

        assertEquals(1, result.rows.size)
        assertEquals("銅鑼灣", result.rows[0].name)
    }

    @Test
    fun `blank lines and comments are ignored`() {
        val result = LocationCsvImport.parse(
            """
            # pack_name: Hong Kong
            # version: 3

            latitude,longitude,name_tc,name_en
            22.28,114.18,銅鑼灣,Causeway Bay

            """.trimIndent()
        )

        assertTrue(result.problems.isEmpty())
        assertEquals(1, result.rows.size)
    }

    @Test
    fun `english-only and chinese-only names are both accepted`() {
        val result = LocationCsvImport.parse(
            """
            22.28,114.18,,Causeway Bay
            22.32,114.17,旺角
            """.trimIndent()
        )

        assertTrue(result.problems.isEmpty())
        assertEquals(2, result.rows.size)
        assertEquals("", result.rows[0].name)
        assertEquals("", result.rows[1].nameEn)
    }

    @Test
    fun `quoted field containing a comma survives`() {
        val result = LocationCsvImport.parse("""1.0,2.0,"甲, 乙","A, B",x""")

        assertEquals("甲, 乙", result.rows[0].name)
        assertEquals("A, B", result.rows[0].nameEn)
    }

    @Test
    fun `out of range coordinates are reported against their line number`() {
        val result = LocationCsvImport.parse(
            """
            latitude,longitude,name_tc,name_en
            91.0,114.18,壞緯度,Bad Lat
            22.28,181.0,壞經度,Bad Lng
            22.28,114.18,好,Good
            """.trimIndent()
        )

        assertEquals(1, result.rows.size)
        assertEquals("好", result.rows[0].name)
        assertEquals(2, result.problems.size)
        assertEquals(2, result.problems[0].lineNumber)
        assertEquals(LocationCsvImport.Reason.BAD_LATITUDE, result.problems[0].reason)
        assertEquals(3, result.problems[1].lineNumber)
        assertEquals(LocationCsvImport.Reason.BAD_LONGITUDE, result.problems[1].reason)
    }

    @Test
    fun `a row missing a name is reported rather than dropped`() {
        val result = LocationCsvImport.parse(
            """
            22.28,114.18,,
            """.trimIndent()
        )

        assertTrue(result.rows.isEmpty())
        assertEquals(LocationCsvImport.Reason.NO_NAME, result.problems.single().reason)
    }

    @Test
    fun `a row with too few columns is reported`() {
        val result = LocationCsvImport.parse("22.28,114.18")

        assertTrue(result.rows.isEmpty())
        assertEquals(LocationCsvImport.Reason.TOO_FEW_COLUMNS, result.problems.single().reason)
    }

    @Test
    fun `junk after the data starts is a problem, not a second header`() {
        val result = LocationCsvImport.parse(
            """
            22.28,114.18,銅鑼灣,Causeway Bay
            not,a,number
            """.trimIndent()
        )

        assertEquals(1, result.rows.size)
        assertEquals(LocationCsvImport.Reason.BAD_LATITUDE, result.problems.single().reason)
        assertEquals(2, result.problems.single().lineNumber)
    }

    @Test
    fun `empty input yields nothing at all`() {
        val result = LocationCsvImport.parse("")

        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.isEmpty())
    }

    @Test
    fun `the shipped example parses cleanly`() {
        // Guards the "fill in example" button: if someone edits EXAMPLE into
        // something the parser rejects, the button starts handing users errors.
        val result = LocationCsvImport.parse(LocationCsvImport.EXAMPLE)

        assertTrue(result.problems.isEmpty())
        assertEquals(2, result.rows.size)
        assertEquals("香港藝術館", result.rows[0].name)
        assertEquals("Kinkaku-ji", result.rows[1].nameEn)
        // Every column populated, which is the point of showing it as a sample.
        assertTrue(result.rows.all { it.tags.isNotBlank() && it.tagsEn.isNotBlank() })
    }

    @Test
    fun `a bad latitude on the very first line is reported, not eaten as a header`() {
        val result = LocationCsvImport.parse("abc,114.18,銅鑼灣")

        assertTrue(result.rows.isEmpty())
        assertEquals(LocationCsvImport.Reason.BAD_LATITUDE, result.problems.single().reason)
        assertEquals(1, result.problems.single().lineNumber)
    }

    @Test
    fun `a broken first line does not vanish when good rows follow`() {
        val result = LocationCsvImport.parse(
            """
            abc,114.18,壞
            22.28,114.18,好
            """.trimIndent()
        )

        assertEquals(1, result.rows.size)
        assertEquals(1, result.problems.single().lineNumber)
    }

    @Test
    fun `a non-english header is still recognised`() {
        val result = LocationCsvImport.parse(
            """
            緯度,經度,名稱
            22.28,114.18,銅鑼灣
            """.trimIndent()
        )

        assertTrue(result.problems.isEmpty())
        assertEquals(1, result.rows.size)
    }

    @Test
    fun `coordinate bounds are inclusive`() {
        val result = LocationCsvImport.parse(
            """
            90,180,北
            -90,-180,南
            """.trimIndent()
        )

        assertTrue(result.problems.isEmpty())
        assertEquals(2, result.rows.size)
    }

    @Test
    fun `whitespace around fields is trimmed`() {
        val result = LocationCsvImport.parse("  22.28 , 114.18 ,  銅鑼灣  ,  Causeway Bay  ")

        assertEquals("銅鑼灣", result.rows[0].name)
        assertEquals("Causeway Bay", result.rows[0].nameEn)
        assertEquals(22.28, result.rows[0].latitude, 1e-9)
    }

    @Test
    fun `windows line endings are handled`() {
        // What a paste out of Excel or Notepad actually carries.
        val result = LocationCsvImport.parse("22.28,114.18,甲\r\n22.32,114.17,乙\r\n")

        assertTrue(result.problems.isEmpty())
        assertEquals(2, result.rows.size)
    }

    @Test
    fun `columns beyond the sixth are ignored`() {
        val result = LocationCsvImport.parse("22.28,114.18,甲,A,tagTc,tagEn,junk,more")

        assertEquals("tagTc", result.rows[0].tags)
        assertEquals("tagEn", result.rows[0].tagsEn)
    }

    @Test
    fun `a header with no rows under it imports nothing and complains about nothing`() {
        val result = LocationCsvImport.parse("latitude,longitude,name_tc,name_en")

        assertTrue(result.rows.isEmpty())
        assertTrue(result.problems.isEmpty())
    }
}
