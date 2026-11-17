package org.unishiasy.vehiclelog.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.LinkedReadingEdit
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.ReminderWriteResult

@RunWith(AndroidJUnit4::class)
class LedgerMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val vehicles = listOf(
        VehicleEntity("v1-a", "car", "پژو ۲۰۶"),
        VehicleEntity("v1-b", "car", "سمند LX", "samand-lx", 1400, "solar_hijri"),
        VehicleEntity("v1-c", "car", "Toyota Camry", null, 2018, "gregorian"),
    )

    @Test
    fun chainsExportedV1ThroughV2AndV3ToV4AndPreservesVehicles() = runBlocking<Unit> {
        val name = "ledger-migration-test-${UUID.randomUUID()}.db"
        try {
            createFixture(name, version = 1)
            val entry = EntryEntity(
                UUID.randomUUID().toString(), vehicles[1].id, "parts", "2026-11-10",
                1_794_268_800_000L, "تعویض قطعه", null, null,
            )
            val database = openLatest(name)
            try {
                assertEquals(vehicles, database.vehicleDao().observeAll().first())
                assertEquals(emptyList<EntryEntity>(), database.ledgerDao().observeForVehicle(entry.vehicleId).first())
                assertNull(database.odometerDao().observeCurrentMileage(entry.vehicleId).first())
                database.ledgerDao().insert(entry)
            } finally {
                database.close()
            }
            val reopened = openLatest(name)
            try {
                assertEquals(vehicles, reopened.vehicleDao().observeAll().first())
                assertEquals(entry, reopened.ledgerDao().findById(entry.id))
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migratesPopulatedV2ThroughV3ToFreshV4SchemaAndPreservesRecordsOnReopen() = runBlocking<Unit> {
        val name = "ledger-migration-test-${UUID.randomUUID()}.db"
        val fuel = EntryEntity(
            UUID.randomUUID().toString(), vehicles[0].id, "fuel", "2026-11-10",
            1_794_268_800_000L, "سوخت", 9_007_199_254_740_993L, 32450,
        )
        val service = fuel.copy(
            id = UUID.randomUUID().toString(), vehicleId = vehicles[1].id,
            category = "parts", notes = null, amountRials = null, fuelVolumeMl = null,
        )
        val expense = fuel.copy(
            id = UUID.randomUUID().toString(), vehicleId = vehicles[2].id,
            category = "insurance", amountRials = 5_000_000, fuelVolumeMl = null,
        )
        val entries = listOf(fuel, service, expense)
        val standaloneId = UUID.randomUUID()
        val standalone = OdometerInput(service.vehicleId, "2026-11-09", 1250)
        try {
            createFixture(name, version = 2, entries = entries)
            val migrated = openLatest(name)
            val linked = try {
                assertVehiclesAndEntries(migrated, entries)
                val fresh = freshDatabase()
                try {
                    assertEquals(ledgerSchema(fresh.openHelper.readableDatabase), ledgerSchema(migrated.openHelper.readableDatabase))
                } finally {
                    fresh.close()
                }
                val repository = LedgerRepository(migrated)
                for (vehicle in vehicles) {
                    assertNull(repository.observeCurrentMileage(vehicle.id).first())
                }
                val serviceId = UUID.fromString(service.id)
                assertEquals(EntrySaveResult.Saved, repository.createStandaloneReading(standaloneId, standalone))
                assertEquals(
                    EntrySaveResult.Saved,
                    repository.update(
                        serviceId, EntryInput(service.vehicleId, service.category, service.occurredOn),
                        LinkedReadingEdit.Set(1500),
                    ),
                )
                checkNotNull(repository.findWithReading(serviceId)?.reading)
            } finally {
                migrated.close()
            }
            val reopened = openLatest(name)
            try {
                assertVehiclesAndEntries(reopened, entries)
                assertEquals(linked, reopened.odometerDao().findForEntry(service.id))
                assertEquals(
                    OdometerReadingEntity(standaloneId.toString(), standalone.vehicleId, standalone.occurredOn, 1250, null),
                    reopened.odometerDao().findById(standaloneId.toString()),
                )
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migratesPopulatedV3ToV4AndPreservesLedgerAndRemindersOnReopen() = runBlocking<Unit> {
        val name = "reminder-migration-test-${UUID.randomUUID()}.db"
        val entry = EntryEntity("fuel", vehicles[0].id, "fuel", "2026-03-21", 42, "سوخت", 120001, 32450)
        val linked = OdometerReadingEntity("linked", entry.vehicleId, entry.occurredOn, 1200, entry.id)
        val standalone = OdometerReadingEntity("standalone", entry.vehicleId, "2026-03-20", 1000, null)
        val id = UUID.randomUUID()
        try {
            createFixture(name, version = 3, entries = listOf(entry), readings = listOf(linked, standalone))
            openLatest(name).useDatabase { migrated ->
                assertEquals(emptyList<ReminderEntity>(), migrated.reminderDao().observeForVehicle(entry.vehicleId).first())
                freshDatabase().useDatabase { fresh ->
                    assertEquals(ledgerSchema(fresh.openHelper.readableDatabase), ledgerSchema(migrated.openHelper.readableDatabase))
                }
                val repository = ReminderRepository(migrated, LedgerRepository(migrated))
                assertEquals(ReminderWriteResult.Saved, repository.create(entry.vehicleId, id, ReminderInput("Service", dueKilometers = 1200)))
                assertEquals(id.toString(), repository.observeDue(entry.vehicleId).first().single().id)
            }
            openLatest(name).useDatabase { reopened ->
                assertVehiclesAndEntries(reopened, listOf(entry))
                assertEquals(linked, reopened.odometerDao().findForEntry(entry.id))
                assertEquals(standalone, reopened.odometerDao().findById(standalone.id))
                assertEquals(id.toString(), reopened.reminderDao().observeForVehicle(entry.vehicleId).first().single().id)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun reminderWritesAndLifecycleAreVehicleScoped() = runBlocking<Unit> {
        freshDatabase().useDatabase { database ->
            val vehicle = vehicles[0]
            val other = vehicles[1]
            database.vehicleDao().insert(vehicle)
            database.vehicleDao().insert(other)
            val repository = ReminderRepository(database, LedgerRepository(database))
            val id = UUID.randomUUID()
            val canceledId = UUID.randomUUID()
            val input = ReminderInput(" Service ", "2020-01-01")
            assertEquals(ReminderWriteResult.InvalidInput, repository.create(vehicle.id, id, ReminderInput("Service")))
            assertEquals(ReminderWriteResult.ConstraintViolation, repository.create("missing", id, input))
            assertEquals(ReminderWriteResult.Saved, repository.create(vehicle.id, id, input))
            assertEquals(ReminderWriteResult.ConstraintViolation, repository.create(vehicle.id, id, input))
            assertEquals(ReminderWriteResult.Saved, repository.create(vehicle.id, canceledId, input))
            assertEquals(2, repository.observeDue(vehicle.id).first().size)
            assertEquals(emptyList<ReminderEntity>(), repository.observeForVehicle(other.id).first())
            assertEquals(ReminderWriteResult.NoActiveReminder, repository.complete(other.id, id))
            assertEquals(ReminderWriteResult.Saved, repository.complete(vehicle.id, id))
            assertEquals(ReminderWriteResult.Saved, repository.cancel(vehicle.id, canceledId))
            assertEquals(ReminderWriteResult.NoActiveReminder, repository.complete(vehicle.id, canceledId))
            assertEquals(emptyList<ReminderEntity>(), repository.observeDue(vehicle.id).first())
            assertEquals(
                mapOf(id.toString() to "completed", canceledId.toString() to "canceled"),
                repository.observeForVehicle(vehicle.id).first().associate { it.id to it.state },
            )
        }
    }

    @Test
    fun sqliteRejectsInvalidReminderRowsAndReferencedVehicleDeletion() = runBlocking<Unit> {
        freshDatabase().useDatabase { database ->
            val vehicle = vehicles[0]
            database.vehicleDao().insert(vehicle)
            val db = database.openHelper.writableDatabase
            fun insert(date: String?, kilometers: Any?, state: String = "active") {
                db.execSQL(
                    "INSERT INTO reminders VALUES (?, ?, 'Service', ?, ?, ?)",
                    arrayOf(UUID.randomUUID().toString(), vehicle.id, date, kilometers, state),
                )
            }
            assertThrows(SQLiteConstraintException::class.java) { insert(null, null) }
            assertThrows(SQLiteConstraintException::class.java) { insert("2028-02-29", 1) }
            assertThrows(SQLiteConstraintException::class.java) { insert("2026-02-29", null) }
            assertThrows(SQLiteConstraintException::class.java) { insert(null, -1) }
            assertThrows(SQLiteConstraintException::class.java) { insert(null, 1.5) }
            assertThrows(SQLiteConstraintException::class.java) { insert(null, 0, "unknown") }
            insert("2028-02-29", null)
            insert(null, 0)
            assertThrows(SQLiteConstraintException::class.java) { db.execSQL("UPDATE reminders SET state = 'unknown'") }
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("DELETE FROM vehicles WHERE id = ?", arrayOf(vehicle.id))
            }
        }
    }

    private inline fun VehicleDatabase.useDatabase(block: (VehicleDatabase) -> Unit) {
        try {
            block(this)
        } finally {
            close()
        }
    }

    private fun freshDatabase() = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
        .addCallback(reminderConstraints)
        .build()

    private suspend fun assertVehiclesAndEntries(database: VehicleDatabase, entries: List<EntryEntity>) {
        assertEquals(vehicles, database.vehicleDao().observeAll().first())
        for (vehicle in vehicles) {
            assertEquals(entries.filter { it.vehicleId == vehicle.id }, database.ledgerDao().observeForVehicle(vehicle.id).first())
        }
    }

    private fun ledgerSchema(database: SupportSQLiteDatabase): Map<String, String> {
        val whitespace = Regex("\\s+")
        return database.query(
            """
            SELECT name, sql FROM sqlite_master
            WHERE tbl_name IN ('vehicles', 'entries', 'odometer_readings', 'reminders') AND sql IS NOT NULL
            """.trimIndent(),
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.getString(0), cursor.getString(1).replace(whitespace, ""))
                }
            }
        }
    }

    private fun openLatest(name: String) = Room.databaseBuilder(context, VehicleDatabase::class.java, name)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .addCallback(reminderConstraints)
        .build()

    private fun createFixture(
        name: String,
        version: Int,
        entries: List<EntryEntity> = emptyList(),
        readings: List<OdometerReadingEntity> = emptyList(),
    ) {
        val asset = "${VehicleDatabase::class.java.name}/$version.json"
        val schema = instrumentation.context.assets.open(asset).bufferedReader().use {
            JSONObject(it.readText()).getJSONObject("database")
        }
        assertEquals(version, schema.getInt("version"))

        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { database ->
            val tables = schema.getJSONArray("entities")
            for (index in 0 until tables.length()) {
                val table = tables.getJSONObject(index)
                val tableName = table.getString("tableName")
                database.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indexes = table.optJSONArray("indices") ?: continue
                for (indexNumber in 0 until indexes.length()) {
                    val sql = indexes.getJSONObject(indexNumber).getString("createSql")
                    database.execSQL(sql.replace("\${TABLE_NAME}", tableName))
                }
            }
            val setupQueries = schema.getJSONArray("setupQueries")
            for (index in 0 until setupQueries.length()) {
                database.execSQL(setupQueries.getString(index))
            }
            for (vehicle in vehicles) {
                database.execSQL(
                    """
                    INSERT INTO vehicles (id, kind, displayLabel, catalogKey, productionYear, productionYearCalendar)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        vehicle.id, vehicle.kind, vehicle.displayLabel,
                        vehicle.catalogKey, vehicle.productionYear, vehicle.productionYearCalendar,
                    ),
                )
            }
            for (entry in entries) {
                database.execSQL(
                    """
                    INSERT INTO entries (id, vehicleId, category, occurredOn, createdAtEpochMillis, notes, amountRials, fuelVolumeMl)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf<Any?>(
                        entry.id, entry.vehicleId, entry.category, entry.occurredOn,
                        entry.createdAtEpochMillis, entry.notes, entry.amountRials, entry.fuelVolumeMl,
                    ),
                )
            }
            for (reading in readings) {
                database.execSQL(
                    "INSERT INTO odometer_readings (id, vehicleId, occurredOn, kilometers, entryId) VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any?>(reading.id, reading.vehicleId, reading.occurredOn, reading.kilometers, reading.entryId),
                )
            }
            database.version = version
        }
    }
}
