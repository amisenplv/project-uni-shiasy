package org.unishiasy.vehiclelog.ui.vehicle

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.unishiasy.vehiclelog.data.VehicleInput
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.VehicleSaveResult
import org.unishiasy.vehiclelog.data.local.VehicleDao
import org.unishiasy.vehiclelog.data.local.VehicleEntity

@OptIn(ExperimentalCoroutinesApi::class)
class VehicleViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun openAndCloseManagementTogglesStateAndClearsTransientDraftAndResult() {
        val model = VehicleViewModel(VehicleRepository(FakeVehicleDao()))
        dispatcher.scheduler.advanceUntilIdle()

        assertFalse(model.state.value.managingList)
        model.openManagement()
        assertTrue(model.state.value.managingList)
        assertNull(model.state.value.saveResult)

        model.addVehicle()
        model.changeInput { VehicleInput("پژو ۲۰۶") }
        model.save()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(model.state.value.saveResult is VehicleSaveResult.Created)
        model.clearSaveResult()
        assertNull(model.state.value.saveResult)

        model.addVehicle()
        model.closeManagement()
        assertFalse(model.state.value.managingList)
        assertNull(model.state.value.draft)
        assertNull(model.state.value.saveResult)
        assertEquals(1, model.state.value.vehicles?.size)
    }

    private class FakeVehicleDao : VehicleDao {
        private val rows = MutableStateFlow<List<VehicleEntity>>(emptyList())

        override fun observeAll(): Flow<List<VehicleEntity>> = rows

        override suspend fun findById(id: String): VehicleEntity? = rows.value.firstOrNull { it.id == id }

        override suspend fun insert(vehicle: VehicleEntity) {
            rows.value = rows.value + vehicle
        }

        override suspend fun update(vehicle: VehicleEntity): Int {
            val index = rows.value.indexOfFirst { it.id == vehicle.id }
            if (index < 0) return 0
            rows.value = rows.value.toMutableList().apply { this[index] = vehicle }
            return 1
        }
    }
}
