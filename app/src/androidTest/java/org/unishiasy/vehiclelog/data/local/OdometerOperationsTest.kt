package org.unishiasy.vehiclelog.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.LinkedReadingEdit
import org.unishiasy.vehiclelog.data.OdometerInput

@RunWith(AndroidJUnit4::class)
class OdometerOperationsTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: VehicleDatabase
    private lateinit var repository: LedgerRepository
    private val vehicle = VehicleEntity("vehicle-a", "car", "سمند")
    private val other = vehicle.copy(id = "vehicle-b")
    private val entryId = UUID.randomUUID()
    private val entry = EntryInput(vehicle.id, "repair", "2026-11-10")
    private val createdAt = 1_794_268_800_000L

    @Before
    fun openDatabase() = runBlocking<Unit> {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).build()
        repository = LedgerRepository(database)
        database.vehicleDao().insert(vehicle)
        database.vehicleDao().insert(other)
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun backdatedInsertsAndEditsUseBothBoundsAndExcludeEditedRow() = runBlocking<Unit> {
        standalone("2026-11-08", 120)
        standalone("2026-11-08", 100)
        standalone("2026-11-12", 220)
        standalone("2026-11-12", 200)
        val id = UUID.randomUUID()
        val input = OdometerInput(vehicle.id, "2026-11-10", 120)
        assertEquals(EntrySaveResult.MileageConflict, repository.createStandaloneReading(id, input.copy(kilometers = 119)))
        assertEquals(EntrySaveResult.MileageConflict, repository.createStandaloneReading(id, input.copy(kilometers = 201)))
        assertNull(repository.findReadingById(id))
        assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(id, input))
        assertEquals(EntrySaveResult.Saved, repository.updateStandaloneReading(id, input.copy(kilometers = 200)))
        assertEquals(EntrySaveResult.MileageConflict, repository.updateStandaloneReading(id, input.copy(kilometers = 201)))

        // The old reading is 200. Moving it one day later at 180 would fail if it bounded itself.
        assertEquals(
            EntrySaveResult.Saved,
            repository.updateStandaloneReading(id, input.copy(occurredOn = "2026-11-11", kilometers = 180)),
        )
        assertEquals(
            OdometerReadingEntity(id.toString(), vehicle.id, "2026-11-11", 180, null),
            repository.findReadingById(id),
        )
    }

    @Test
    fun currentMileageUsesLatestDayMaximumAndAcceptsZeroAndLongMaximum() = runBlocking<Unit> {
        val current = repository.observeCurrentMileage(vehicle.id)
        assertNull(current.first())
        val id = UUID.randomUUID()
        val zero = OdometerInput(vehicle.id, "2099-01-01", 0)
        assertEquals(EntrySaveResult.InvalidKilometers, repository.createStandaloneReading(id, zero.copy(kilometers = -1)))
        assertEquals(
            EntrySaveResult.InvalidOccurrenceDate,
            repository.createStandaloneReading(id, zero.copy(occurredOn = "2099-02-29")),
        )
        assertEquals(EntrySaveResult.InvalidKilometers, repository.create(entryId, createdAt, entry, -1))
        assertNull(repository.findById(entryId))
        assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(id, zero))
        assertEquals(0L, current.first())
        val maximum = standalone("2099-01-02", Long.MAX_VALUE)
        val sameDay = standalone("2099-01-02", 42)
        assertEquals(Long.MAX_VALUE, current.first())
        assertEquals(EntrySaveResult.Saved, repository.deleteStandaloneReading(maximum))
        assertEquals(42L, current.first())
        assertEquals(EntrySaveResult.Saved, repository.deleteStandaloneReading(sameDay))
        assertEquals(0L, current.first())
        assertEquals(EntrySaveResult.Saved, repository.deleteStandaloneReading(id))
        assertEquals(EntrySaveResult.NotFound, repository.deleteStandaloneReading(id))
        assertNull(current.first())
    }

    @Test
    fun linkedDateAndVehicleMovesValidateDestinationAndPreserveIds() = runBlocking<Unit> {
        standalone("2026-11-12", 200)
        standalone("2026-11-09", 300, other.id)
        assertEquals(EntrySaveResult.Saved, repository.create(entryId, createdAt, entry, 100))
        val before = checkNotNull(repository.findWithReading(entryId))
        val beforeReading = checkNotNull(before.reading)
        assertEquals(
            EntrySaveResult.MileageConflict,
            repository.update(entryId, entry.copy(occurredOn = "2026-11-13"), LinkedReadingEdit.Keep),
        )
        assertEquals(before, repository.findWithReading(entryId))
        val backdated = entry.copy(occurredOn = "2026-11-08")
        assertEquals(EntrySaveResult.Saved, repository.update(entryId, backdated))
        val moved = backdated.copy(vehicleId = other.id, occurredOn = "2026-11-10")
        assertEquals(EntrySaveResult.ReadingRequiresVehicleConfirmation, repository.update(entryId, moved))
        assertEquals(EntrySaveResult.MileageConflict, repository.update(entryId, moved, LinkedReadingEdit.Set(299)))
        assertEquals(beforeReading.copy(occurredOn = backdated.occurredOn), repository.findWithReading(entryId)?.reading)
        assertEquals(EntrySaveResult.Saved, repository.update(entryId, moved, LinkedReadingEdit.Set(350)))
        val after = checkNotNull(repository.findWithReading(entryId))
        assertEquals(before.entry.copy(vehicleId = other.id, occurredOn = moved.occurredOn), after.entry)
        assertEquals(beforeReading.copy(vehicleId = other.id, occurredOn = moved.occurredOn, kilometers = 350), after.reading)
        assertEquals(EntrySaveResult.Saved, repository.update(entryId, moved, LinkedReadingEdit.Set(350)))
        assertEquals(after, repository.findWithReading(entryId))
        assertEquals(200L, repository.observeCurrentMileage(vehicle.id).first())
        assertEquals(350L, repository.observeCurrentMileage(other.id).first())
    }

    @Test
    fun identicalCreatesAreSafeAndConflictingDuplicatesPreserveBothRows() = runBlocking<Unit> {
        assertEquals(EntrySaveResult.Saved, repository.create(entryId, createdAt, entry, 10))
        val saved = checkNotNull(repository.findWithReading(entryId))
        assertEquals(EntrySaveResult.Saved, repository.create(entryId, createdAt, entry, 10))
        assertEquals(EntrySaveResult.ConstraintViolation, repository.create(entryId, createdAt, entry, 11))
        assertEquals(EntrySaveResult.ConstraintViolation, repository.create(entryId, createdAt, entry))
        assertEquals(
            EntrySaveResult.ConstraintViolation,
            repository.create(entryId, createdAt, entry.copy(notes = "edited"), 10),
        )
        assertEquals(saved, repository.findWithReading(entryId))
        val id = UUID.randomUUID()
        val reading = OdometerInput(vehicle.id, entry.occurredOn, 9)
        assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(id, reading))
        assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(id, reading))
        assertEquals(
            EntrySaveResult.ConstraintViolation,
            repository.createStandaloneReading(id, reading.copy(kilometers = 10)),
        )
        assertEquals(9L, repository.findReadingById(id)?.kilometers)
    }

    @Test
    fun removalPreservesEntryAndCascadeDeletesOnlyTheLinkedReading() = runBlocking<Unit> {
        val standalone = standalone(entry.occurredOn, 70)
        val siblingId = UUID.randomUUID()
        assertEquals(EntrySaveResult.Saved, repository.create(siblingId, createdAt, entry, 80))
        val sibling = checkNotNull(repository.findWithReading(siblingId))
        assertEquals(EntrySaveResult.Saved, repository.create(entryId, createdAt, entry, 100))
        val linked = checkNotNull(repository.findWithReading(entryId)?.reading)
        val linkedId = UUID.fromString(linked.id)
        val input = OdometerInput(vehicle.id, entry.occurredOn, 101)
        assertEquals(EntrySaveResult.NotFound, repository.updateStandaloneReading(linkedId, input))
        assertEquals(EntrySaveResult.NotFound, repository.deleteStandaloneReading(linkedId))
        assertEquals(
            EntrySaveResult.Saved,
            repository.update(entryId, entry.copy(vehicleId = other.id), LinkedReadingEdit.Remove),
        )
        assertEquals(other.id, repository.findById(entryId)?.vehicleId)
        assertNull(repository.findReadingById(linkedId))
        assertNull(repository.findWithReading(entryId)?.reading)
        assertEquals(EntrySaveResult.Saved, repository.update(entryId, entry, LinkedReadingEdit.Set(90)))
        val newLink = checkNotNull(repository.findWithReading(entryId)?.reading)
        assertEquals(90L, repository.observeCurrentMileage(vehicle.id).first())
        assertEquals(EntrySaveResult.Saved, repository.deleteEntry(entryId))
        assertEquals(EntrySaveResult.NotFound, repository.deleteEntry(entryId))
        assertNull(repository.findById(entryId))
        assertNull(repository.findReadingById(UUID.fromString(newLink.id)))
        assertEquals(sibling, repository.findWithReading(siblingId))
        assertEquals(70L, repository.findReadingById(standalone)?.kilometers)
        assertEquals(80L, repository.observeCurrentMileage(vehicle.id).first())
    }

    @Test
    fun sqliteEnforcesBothForeignKeysAndUniqueLinkAndRestrictsVehicleDeletion() = runBlocking<Unit> {
        val dao = database.odometerDao()
        val raw = OdometerReadingEntity(UUID.randomUUID().toString(), vehicle.id, entry.occurredOn, 10, null)
        assertConstraint { dao.insert(raw.copy(vehicleId = "missing")) }
        assertConstraint { dao.insert(raw.copy(entryId = "missing")) }
        assertEquals(EntrySaveResult.Saved, repository.create(entryId, createdAt, entry, 10))
        assertConstraint { dao.insert(raw.copy(entryId = entryId.toString())) }
        dao.insert(raw)
        dao.insert(raw.copy(id = UUID.randomUUID().toString()))
        standalone(entry.occurredOn, 10, other.id)
        assertConstraint { execSql("DELETE FROM vehicles WHERE id = ?", arrayOf(other.id)) }
        assertEquals(other, database.vehicleDao().findById(other.id))
    }

    @Test
    fun secondWriteFailureRollsBackEntryCreationAndEdit() = runBlocking<Unit> {
        val changed = entry.copy(notes = "changed")
        rejectReadingWrite("INSERT")
        assertEquals(EntrySaveResult.ConstraintViolation, repository.create(entryId, createdAt, changed, 10))
        assertNull(repository.findWithReading(entryId))
        assertNull(repository.observeCurrentMileage(vehicle.id).first())
        execSql("DROP TRIGGER reject_reading_write")

        assertEquals(EntrySaveResult.Saved, repository.create(entryId, createdAt, entry, 10))
        val before = repository.findWithReading(entryId)
        rejectReadingWrite("UPDATE")
        assertEquals(
            EntrySaveResult.ConstraintViolation,
            repository.update(entryId, changed, LinkedReadingEdit.Set(11)),
        )
        assertEquals(before, repository.findWithReading(entryId))
        assertEquals(10L, repository.observeCurrentMileage(vehicle.id).first())
    }

    @Test
    fun competingWritesCannotBothPassIncompatibleBounds() = runBlocking<Unit> {
        val results = listOf(
            async { repository.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicle.id, "2026-11-08", 200)) },
            async { repository.createStandaloneReading(UUID.randomUUID(), OdometerInput(vehicle.id, "2026-11-12", 100)) },
        ).awaitAll()
        assertEquals(1, results.count { it == EntrySaveResult.Saved })
        assertEquals(1, results.count { it == EntrySaveResult.MileageConflict })
    }

    private suspend fun standalone(day: String, kilometers: Long, vehicleId: String = vehicle.id): UUID {
        val id = UUID.randomUUID()
        assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(id, OdometerInput(vehicleId, day, kilometers)))
        return id
    }

    private suspend fun rejectReadingWrite(operation: String) = execSql(
        """
        CREATE TRIGGER reject_reading_write BEFORE $operation ON odometer_readings
        WHEN EXISTS (SELECT 1 FROM entries WHERE id = NEW.entryId AND notes = 'changed')
        BEGIN SELECT RAISE(ABORT, 'forced second write failure'); END
        """.trimIndent(),
    )

    private suspend fun execSql(sql: String, args: Array<Any> = emptyArray()) = withContext(Dispatchers.IO) {
        database.openHelper.writableDatabase.execSQL(sql, args)
    }

    private suspend fun assertConstraint(write: suspend () -> Unit) {
        try {
            write()
            fail("Expected SQLite to reject the write")
        } catch (_: SQLiteConstraintException) {
            // The constraint is enforced by SQLite.
        }
    }
}
