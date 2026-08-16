package com.gpsanywhere.app

import com.gpsanywhere.app.data.LocationCsvImport
import com.gpsanywhere.app.data.LocationCsvImport.isSameAs
import com.gpsanywhere.app.data.SavedLocation
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rule that stops a re-pasted CSV from doubling every location.
 *
 * Room cannot enforce this: onConflict=IGNORE keys off the unique sourceId
 * index, and every imported row carries a null sourceId.
 */
class LocationCsvDuplicateTest {

    private fun row(
        name: String = "銅鑼灣",
        nameEn: String = "Causeway Bay",
        lat: Double = 22.28,
        lng: Double = 114.18
    ) = LocationCsvImport.Row(1, name, nameEn, lat, lng, "", "")

    private fun saved(
        name: String = "銅鑼灣",
        nameEn: String = "Causeway Bay",
        lat: Double = 22.28,
        lng: Double = 114.18
    ) = SavedLocation(name = name, nameEn = nameEn, latitude = lat, longitude = lng)

    @Test
    fun `same name and position is a duplicate`() {
        assertTrue(row().isSameAs(saved()))
    }

    @Test
    fun `name matching is case insensitive and ignores surrounding space`() {
        assertTrue(row(nameEn = "  causeway bay ").isSameAs(saved(nameEn = "Causeway Bay")))
    }

    @Test
    fun `matching either language is enough`() {
        assertTrue(row(name = "銅鑼灣", nameEn = "Different").isSameAs(saved(nameEn = "Other")))
        assertTrue(row(name = "別的", nameEn = "Causeway Bay").isSameAs(saved(name = "另一個")))
    }

    @Test
    fun `a different name at the same spot is not a duplicate`() {
        assertFalse(row(name = "維園", nameEn = "Victoria Park").isSameAs(saved()))
    }

    @Test
    fun `two english-only locations sharing coordinates are not fused`() {
        // Both carry name = "", so a rule comparing only the Chinese name would
        // call these the same place.
        val a = row(name = "", nameEn = "Foo")
        val b = saved(name = "", nameEn = "Bar")
        assertFalse(a.isSameAs(b))
    }

    @Test
    fun `a blank name never matches a blank name`() {
        assertFalse(row(name = "", nameEn = "Foo").isSameAs(saved(name = "", nameEn = "")))
    }

    @Test
    fun `the same name a long way away is not a duplicate`() {
        assertFalse(row(lat = 35.0394, lng = 135.7292).isSameAs(saved()))
    }

    @Test
    fun `positions within about a metre count as the same place`() {
        val nudged = LocationCsvImport.DUPLICATE_EPSILON / 2
        assertTrue(row(lat = 22.28 + nudged, lng = 114.18 + nudged).isSameAs(saved()))
    }

    @Test
    fun `positions beyond the epsilon are distinct`() {
        val moved = LocationCsvImport.DUPLICATE_EPSILON * 2
        assertFalse(row(lat = 22.28 + moved).isSameAs(saved()))
        assertFalse(row(lng = 114.18 + moved).isSameAs(saved()))
    }
}
