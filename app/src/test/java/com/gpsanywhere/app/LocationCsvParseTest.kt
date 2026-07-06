package com.gpsanywhere.app

import com.gpsanywhere.app.data.DefaultLocationSeeder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationCsvParseTest {

    @Test
    fun `parses a location pack and derives source id from english name`() {
        val csv = """
            # pack_name: Japan
            # version: 2
            latitude,longitude,name_tc,name_en,tags
            36.2048,138.2529,日本,Japan Center,tourist|nature
        """.trimIndent()

        val pack = DefaultLocationSeeder.parseCsv(csv)!!
        assertEquals("Japan", pack.packName)
        assertEquals(2, pack.version)
        assertEquals(1, pack.locations.size)
        val loc = pack.locations[0]
        assertEquals("japan-center", loc.sourceId)   // slugified from name_en
        assertEquals("日本", loc.name)
        assertEquals("Japan Center", loc.nameEng)
        assertEquals("tourist|nature", loc.tags)
    }

    @Test
    fun `quoted field containing a comma is kept intact`() {
        val csv = """
            # pack_name: Q
            latitude,longitude,name_tc,name_en,tags
            1.0,2.0,"甲, 乙","A, B",x
        """.trimIndent()
        val loc = DefaultLocationSeeder.parseCsv(csv)!!.locations[0]
        assertEquals("A, B", loc.nameEng)
        assertEquals("甲, 乙", loc.name)
    }

    @Test
    fun `tags are optional`() {
        val csv = """
            # pack_name: NoTags
            latitude,longitude,name_tc,name_en
            1.0,2.0,甲,A
        """.trimIndent()
        assertEquals("", DefaultLocationSeeder.parseCsv(csv)!!.locations[0].tags)
    }

    @Test
    fun `returns null without a pack name`() {
        assertNull(DefaultLocationSeeder.parseCsv("latitude,longitude,name_tc,name_en\n1.0,2.0,甲,A"))
    }
}
