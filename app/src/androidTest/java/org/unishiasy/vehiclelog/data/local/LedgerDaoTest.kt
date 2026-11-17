package org.unishiasy.vehiclelog.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.VehicleMonthlyReport
import org.unishiasy.vehiclelog.domain.JalaliMonth

@RunWith(AndroidJUnit4::class)
class LedgerDaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: VehicleDatabase
    private lateinit var dao: LedgerDao
    private lateinit var repository: LedgerRepository
    private val vehicle = VehicleEntity(id = "vehicle-a", kind = "car", displayLabel = "سمند")
    private val otherVehicle = vehicle.copy(id = "vehicle-b")
    private val id = UUID.fromString("8d44fed9-6136-4e3b-a1a5-f2a58fe50001")
    private val fuel = EntryInput(vehicle.id, "fuel", "2026-11-10", 9_007_199_254_740_993L, "32.450", " ")
    private val createdAt = 1_794_268_800_000L

    @Before
    fun openDatabase() = runBlocking<Unit> {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).build()
        dao = database.ledgerDao()
        repository = LedgerRepository(database)
        database.vehicleDao().insert(vehicle)
        database.vehicleDao().insert(otherVehicle)
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun readsOnlyRequestedVehicleInDeterministicOrder() = runBlocking<Unit> {
        val first = storedFuel()
        val tied = first.copy(id = "8d44fed9-6136-4e3b-a1a5-f2a58fe50002")
        val earlierSameDay = first.copy(id = UUID.randomUUID().toString(), createdAtEpochMillis = createdAt - 1)
        val backdated = first.copy(id = UUID.randomUUID().toString(), occurredOn = "2026-11-09")
        val other = first.copy(id = UUID.randomUUID().toString(), vehicleId = otherVehicle.id)
        listOf(tied, backdated, other, earlierSameDay, first).forEach { dao.insert(it) }

        assertEquals(listOf(first, tied, earlierSameDay, backdated), repository.observeForVehicle(vehicle.id).first())
        assertEquals(listOf(other), repository.observeForVehicle(otherVehicle.id).first())
    }

    @Test
    fun monthlyReportObservesVehicleScopedRoomChanges() = runBlocking<Unit> {
        dao.insert(storedFuel().copy(vehicleId = otherVehicle.id, occurredOn = "2026-04-21"))
        withTimeout(5_000) {
            val reports = Channel<VehicleMonthlyReport>(Channel.UNLIMITED)
            val observation = launch {
                repository.observeMonthlyReport(vehicle.id).collect { reports.send(it) }
            }
            try {
                assertEquals(VehicleMonthlyReport(emptyList(), emptyList()), reports.receive())
                val result = repository.create(
                    UUID.randomUUID(), createdAt, EntryInput(vehicle.id, "repair", "2026-04-21", 123L),
                )
                assertEquals(EntrySaveResult.Saved, result)
                val month = reports.receive().months.single()
                assertEquals(JalaliMonth(1405, 2), month.month)
                assertEquals(1, month.totals.entryCount)
                assertEquals(123L, month.totals.totalRialsAsLongOrNull())
            } finally {
                observation.cancel()
            }
        }
    }

    @Test
    fun foreignKeyRejectsMissingVehicleOnInsertAndUpdateAndRestrictsDeletion() = runBlocking<Unit> {
        assertConstraint { dao.insert(storedFuel().copy(vehicleId = "missing")) }
        assertNull(dao.findById(id.toString()))
        dao.insert(storedFuel())

        assertEquals(EntrySaveResult.ConstraintViolation, repository.update(id, fuel.copy(vehicleId = "missing")))
        assertConstraint {
            withContext(Dispatchers.IO) {
                database.openHelper.writableDatabase.execSQL("DELETE FROM vehicles WHERE id = ?", arrayOf(vehicle.id))
            }
        }
        assertEquals(vehicle, database.vehicleDao().findById(vehicle.id))
        assertEquals(storedFuel(), repository.findById(id))
    }

    @Test
    fun duplicateInsertAbortsWithoutOverwritingOriginal() = runBlocking<Unit> {
        dao.insert(storedFuel())
        assertConstraint { dao.insert(storedFuel().copy(amountRials = 1)) }
        assertEquals(EntrySaveResult.ConstraintViolation, repository.create(id, createdAt + 1, fuel.copy(amountRials = 1)))
        assertEquals(storedFuel(), repository.findById(id))
    }

    @Test
    fun updateReportsNotFoundAndPreservesCreationInstantOnSuccessfulEdit() = runBlocking<Unit> {
        val service = fuel.copy(category = "repair", amountRials = null, fuelVolumeLiters = null, notes = "  تعویض  ")
        assertEquals(EntrySaveResult.NotFound, repository.update(id, service))
        assertNull(repository.findById(id))

        assertEquals(EntrySaveResult.Saved, repository.create(id, createdAt, fuel))
        assertEquals(storedFuel(), repository.findById(id))
        assertEquals(EntrySaveResult.Saved, repository.update(id, service))
        assertEquals(
            storedFuel().copy(category = "repair", amountRials = null, fuelVolumeMl = null, notes = "  تعویض  "),
            repository.findById(id),
        )
    }

    @Test
    fun acceptsUnpricedServicesAndPositiveAmountsForAllCategories() = runBlocking<Unit> {
        for (category in listOf("oil_change", "repair", "parts")) {
            val serviceId = UUID.randomUUID()
            val service = fuel.copy(category = category, amountRials = null, fuelVolumeLiters = null)
            assertEquals(category, EntrySaveResult.Saved, repository.create(serviceId, createdAt, service))
            assertNull(repository.findById(serviceId)!!.amountRials)
            assertEquals(EntrySaveResult.Saved, repository.update(serviceId, service.copy(amountRials = 1L)))
        }
        for (category in listOf("insurance", "wash", "other")) {
            val expense = fuel.copy(category = category, fuelVolumeLiters = null)
            assertEquals(category, EntrySaveResult.Saved, repository.create(UUID.randomUUID(), createdAt, expense))
        }
        assertEquals(EntrySaveResult.Saved, repository.create(id, createdAt, fuel))
    }

    @Test
    fun invalidCategoryAmountVolumeAndDateLeaveDatabaseUnchanged() = runBlocking<Unit> {
        val invalid = listOf(
            fuel.copy(category = "Fuel") to EntrySaveResult.InvalidCategory,
            fuel.copy(occurredOn = "2025-02-29") to EntrySaveResult.InvalidOccurrenceDate,
            fuel.copy(fuelVolumeLiters = null) to EntrySaveResult.InvalidFuelVolume,
            fuel.copy(fuelVolumeLiters = "1.0001") to EntrySaveResult.InvalidFuelVolume,
            fuel.copy(fuelVolumeLiters = "0") to EntrySaveResult.InvalidFuelVolume,
        ) + listOf("insurance", "wash", "other", "fuel").map { category ->
            fuel.copy(category = category, amountRials = null) to EntrySaveResult.InvalidAmount
        } + listOf("oil_change", "repair", "parts", "insurance", "wash", "other").map { category ->
            fuel.copy(category = category) to EntrySaveResult.InvalidFuelVolume
        } + listOf(0L, -1L).map { amount ->
            fuel.copy(category = "parts", amountRials = amount, fuelVolumeLiters = null) to EntrySaveResult.InvalidAmount
        }
        for ((input, expected) in invalid) {
            assertEquals(input.toString(), expected, repository.create(id, createdAt, input))
        }
        assertNull(repository.findById(id))
        assertEquals(EntrySaveResult.Saved, repository.create(id, createdAt, fuel))
        for ((input, expected) in invalid) {
            assertEquals(input.toString(), expected, repository.update(id, input))
        }
        assertEquals(storedFuel(), repository.findById(id))
    }

    @Test
    fun historyQueryReturnsMoreThanFiveEntriesWithDeterministicOrderAndLinkedOdometer() = runBlocking<Unit> {
        val entryIds = (1..7).map { UUID.fromString("8d44fed9-6136-4e3b-a1a5-f2a58fe5000$it") }
        val entries = listOf(
            EntryEntity(entryIds[0].toString(), vehicle.id, "fuel", "2026-11-10", createdAt, null, 100_000L, 30_000L),
            EntryEntity(entryIds[1].toString(), vehicle.id, "wash", "2026-11-10", createdAt, null, 50_000L, null),
            EntryEntity(entryIds[2].toString(), vehicle.id, "oil_change", "2026-11-10", createdAt - 1000, null, null, null),
            EntryEntity(entryIds[3].toString(), vehicle.id, "repair", "2026-11-09", createdAt, null, 200_000L, null),
            EntryEntity(entryIds[4].toString(), vehicle.id, "parts", "2026-11-08", createdAt, null, 150_000L, null),
            EntryEntity(entryIds[5].toString(), vehicle.id, "insurance", "2026-11-07", createdAt, null, 300_000L, null),
            EntryEntity(entryIds[6].toString(), vehicle.id, "other", "2026-11-06", createdAt, null, 80_000L, null),
        )
        entries.reversed().forEach { dao.insert(it) }

        val otherEntryId = UUID.randomUUID()
        dao.insert(EntryEntity(otherEntryId.toString(), otherVehicle.id, "fuel", "2026-11-10", createdAt, null, 100_000L, 20_000L))

        database.odometerDao().insert(OdometerReadingEntity(UUID.randomUUID().toString(), vehicle.id, "2026-11-10", 1200L, entryIds[0].toString()))
        database.odometerDao().insert(OdometerReadingEntity(UUID.randomUUID().toString(), vehicle.id, "2026-11-09", 1000L, entryIds[3].toString()))
        database.odometerDao().insert(OdometerReadingEntity(UUID.randomUUID().toString(), vehicle.id, "2026-11-05", 900L, null))

        val history = repository.observeHistoryForVehicle(vehicle.id).first()
        assertEquals(7, history.size)
        assertEquals(entryIds[0].toString(), history[0].id)
        assertEquals(1200L, history[0].linkedOdometerKm)
        assertEquals(entryIds[1].toString(), history[1].id)
        assertNull(history[1].linkedOdometerKm)
        assertEquals(entryIds[2].toString(), history[2].id)
        assertEquals(entryIds[3].toString(), history[3].id)
        assertEquals(1000L, history[3].linkedOdometerKm)
        assertEquals(entryIds[4].toString(), history[4].id)
        assertEquals(entryIds[5].toString(), history[5].id)
        assertEquals(entryIds[6].toString(), history[6].id)

        val otherHistory = repository.observeHistoryForVehicle(otherVehicle.id).first()
        assertEquals(1, otherHistory.size)
        assertEquals(otherEntryId.toString(), otherHistory[0].id)
    }

    @Test
    fun deleteEntryCascadesToLinkedReadingAndPreservesStandaloneReadingAndVehicle() = runBlocking<Unit> {
        val entryId = UUID.randomUUID()
        val standaloneId = UUID.randomUUID()

        assertEquals(EntrySaveResult.Saved, repository.create(
            entryId, createdAt, fuel, kilometers = 1500L,
        ))
        assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(
            standaloneId, OdometerInput(vehicle.id, "2026-11-08", 1000L),
        ))

        assertEquals(1500L, repository.observeCurrentMileage(vehicle.id).first())
        val linkedReading = checkNotNull(repository.findWithReading(entryId)?.reading)
        assertEquals(1500L, linkedReading.kilometers)

        val result = repository.deleteEntry(vehicle.id, entryId)
        assertEquals(EntrySaveResult.Saved, result)

        assertNull(repository.findById(entryId))
        assertNull(repository.findWithReading(entryId))
        assertNull(repository.findReadingById(UUID.fromString(linkedReading.id)))

        val standalone = repository.findReadingById(standaloneId)
        assertNotNull(standalone)
        assertEquals(1000L, standalone?.kilometers)

        assertNotNull(database.vehicleDao().findById(vehicle.id))
        assertEquals(1000L, repository.observeCurrentMileage(vehicle.id).first())
    }

    @Test
    fun deleteScopedToVehicleRejectsMismatchedVehicleAndMissingEntry() = runBlocking<Unit> {
        assertEquals(EntrySaveResult.Saved, repository.create(id, createdAt, fuel))
        assertEquals(storedFuel(), repository.findById(id))

        assertEquals(EntrySaveResult.NotFound, repository.deleteEntry(otherVehicle.id, id))
        assertEquals(storedFuel(), repository.findById(id))

        assertEquals(EntrySaveResult.NotFound, repository.deleteEntry(vehicle.id, UUID.randomUUID()))
        assertEquals(storedFuel(), repository.findById(id))

        assertEquals(EntrySaveResult.Saved, repository.deleteEntry(vehicle.id, id))
        assertNull(repository.findById(id))
    }

    private fun storedFuel() = EntryEntity(
        id.toString(), vehicle.id, "fuel", "2026-11-10", createdAt, null, fuel.amountRials, 32450L,
    )

    private suspend fun assertConstraint(write: suspend () -> Unit) {
        try {
            write()
            fail("Expected SQLite to reject the write")
        } catch (_: SQLiteConstraintException) {
            // Verify the resulting rows in the calling test.
        }
    }
}
