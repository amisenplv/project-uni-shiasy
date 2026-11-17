package org.unishiasy.vehiclelog.data.catalog

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VehicleCatalogAvailabilityTest {
    @Test
    fun unconfiguredCatalogDoesNotOpenAnAsset() {
        for (path in listOf(null, "")) {
            assertNull(VehicleCatalogLoader.loadIfAvailable(path) { error("Unexpected asset read") })
        }
    }

    @Test
    fun missingMalformedAndEmptyAssetsKeepCatalogUnavailable() {
        assertNull(VehicleCatalogLoader.loadIfAvailable("catalog.json") { throw IOException("Missing") })
        assertNull(VehicleCatalogLoader.loadIfAvailable("catalog.json") { "invalid".reader() })
        assertNull(VehicleCatalogLoader.loadIfAvailable("catalog.json") {
            """{"schemaVersion":1,"entries":[]}""".reader()
        })
    }

    @Test
    fun configuredValidAssetSuppliesOriginalLabelsAndKeys() {
        val catalog = checkNotNull(VehicleCatalogLoader.loadIfAvailable("catalog.json") { path ->
            assertEquals("catalog.json", path)
            checkNotNull(javaClass.getResourceAsStream("/vehicle_catalog_fixture.json")).bufferedReader()
        })
        val entry = catalog.search("sky بادبادك ٢٠٣").single()
        assertEquals("fixture-sky-kite", entry.key)
        assertEquals("آزمایشی خیال‌گرد بادبادک ۲۰۳ VX", entry.displayLabel)
    }
}
