package org.unishiasy.vehiclelog.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.VehicleEntity

class HomeStateTest {
    private val vehicle1 = VehicleEntity("v-1", "car", "سمند LX", productionYear = 1400, productionYearCalendar = "solar_hijri")
    private val vehicle2 = VehicleEntity("v-2", "car", "پژو ۲۰۶ SD", productionYear = 1402, productionYearCalendar = "solar_hijri")
    private val vehicle3 = VehicleEntity("v-3", "truck", "وانت پراید", productionYear = 1399, productionYearCalendar = "solar_hijri")

    @Test
    fun zeroVehiclesReturnsNullSelectedVehicle() {
        val state = HomeUiState(vehicles = emptyList())
        assertNull(state.selectedVehicle)
        assertNull(state.selectedVehicleMileage)
        assertFalse(state.mileageLoaded)
        assertFalse(state.listFailed)
    }

    @Test
    fun singleVehicleIsAutoSelectedWhenIdNull() {
        val state = HomeUiState(vehicles = listOf(vehicle1))
        assertEquals(vehicle1, state.selectedVehicle)
    }

    @Test
    fun multipleVehiclesDefaultsToFirstVehicleWhenNoSelection() {
        val state = HomeUiState(vehicles = listOf(vehicle1, vehicle2, vehicle3), selectedVehicleId = null)
        assertEquals(vehicle1, state.selectedVehicle)
    }

    @Test
    fun explicitSelectionReturnsCorrespondingVehicle() {
        val state = HomeUiState(vehicles = listOf(vehicle1, vehicle2, vehicle3), selectedVehicleId = "v-2")
        assertEquals(vehicle2, state.selectedVehicle)
    }

    @Test
    fun staleSelectionFallsBackToFirstVehicle() {
        val state = HomeUiState(vehicles = listOf(vehicle1, vehicle2), selectedVehicleId = "deleted-vehicle-id")
        assertEquals(vehicle1, state.selectedVehicle)
    }

    @Test
    fun currentMileageStateDistinguishesNoReadingFromRecordedReading() {
        val noReading = HomeUiState(
            vehicles = listOf(vehicle1),
            selectedVehicleId = "v-1",
            selectedVehicleMileage = null,
            mileageLoaded = true,
        )
        assertNull(noReading.selectedVehicleMileage)
        assertTrue(noReading.mileageLoaded)

        val withReading = HomeUiState(
            vehicles = listOf(vehicle1),
            selectedVehicleId = "v-1",
            selectedVehicleMileage = 125000L,
            mileageLoaded = true,
        )
        assertEquals(125000L, withReading.selectedVehicleMileage)
        assertTrue(withReading.mileageLoaded)
    }
}
