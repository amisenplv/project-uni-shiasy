package org.unishiasy.vehiclelog.data

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.unishiasy.vehiclelog.data.local.VehicleDao
import org.unishiasy.vehiclelog.data.local.VehicleEntity

class VehicleRepositoryTest {
    private val dao = FakeVehicleDao()
    private val repository = VehicleRepository(dao)
    private val input = VehicleInput(displayLabel = "پژو")

    @Test
    fun createsCanonicalUuidAndKeepsItOnUpdate() = runBlocking<Unit> {
        val created = repository.create(input) as VehicleSaveResult.Created

        assertEquals(UUID.fromString(created.id).toString(), created.id)
        assertEquals(VehicleSaveResult.Updated(created.id), repository.update(created.id, VehicleInput(" سمند ")))
        assertEquals(VehicleEntity(created.id, "car", "سمند"), repository.findById(created.id))
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun invalidInputLeavesExistingRowUntouched() = runBlocking<Unit> {
        val created = repository.create(input) as VehicleSaveResult.Created

        assertEquals(
            VehicleSaveResult.InvalidInput(VehicleInputError.BlankDisplayLabel),
            repository.update(created.id, VehicleInput(" ")),
        )
        assertEquals(input.displayLabel, repository.findById(created.id)?.displayLabel)
    }

    @Test
    fun zeroRowsIsNotFoundAndDoesNotInsert() = runBlocking<Unit> {
        assertEquals(VehicleSaveResult.NotFound, repository.update("missing-id", input))
        assertNull(repository.findById("missing-id"))
    }

    @Test
    fun storageFailureIsReportedWithoutReplacingExistingRow() = runBlocking<Unit> {
        val created = repository.create(input) as VehicleSaveResult.Created
        dao.writeFailure = IllegalStateException("Database unavailable")

        assertEquals(VehicleSaveResult.StorageFailure, repository.update(created.id, VehicleInput("سمند")))
        assertEquals(VehicleSaveResult.StorageFailure, repository.create(input))
        assertEquals(input.displayLabel, repository.findById(created.id)?.displayLabel)
        assertEquals(1, dao.rows.size)
    }

    @Test
    fun cancellationIsPropagated() = runBlocking<Unit> {
        val cancellation = CancellationException("Cancelled")
        dao.writeFailure = cancellation

        try {
            repository.create(input)
            fail("Expected cancellation to propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    private class FakeVehicleDao : VehicleDao {
        val rows = mutableMapOf<String, VehicleEntity>()
        var writeFailure: IllegalStateException? = null

        override fun observeAll() = flowOf(rows.values.toList())

        override suspend fun findById(id: String) = rows[id]

        override suspend fun insert(vehicle: VehicleEntity) {
            writeFailure?.let { throw it }
            check(vehicle.id !in rows)
            rows[vehicle.id] = vehicle
        }

        override suspend fun update(vehicle: VehicleEntity): Int {
            writeFailure?.let { throw it }
            if (vehicle.id !in rows) return 0
            rows[vehicle.id] = vehicle
            return 1
        }
    }
}
