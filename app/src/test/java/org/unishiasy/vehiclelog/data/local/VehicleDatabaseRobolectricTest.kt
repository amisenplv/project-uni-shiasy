package org.unishiasy.vehiclelog.data.local

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.ReminderWriteResult
import org.unishiasy.vehiclelog.data.backup.BackupOutcome
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.reminders.ReminderNotifications

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VehicleDatabaseRobolectricTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val vehicles = listOf(
        VehicleEntity("v1-a", "car", "پژو ۲۰۶"),
        VehicleEntity("v1-b", "car", "سمند LX", "samand-lx", 1400, "solar_hijri"),
    )

    @Test
    fun migratesPopulatedV1ThroughV4AndMatchesFreshV4Schema() = runBlocking<Unit> {
        val dbName = "robolectric-migration-${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()
        try {
            createFixtureFromSchema(dbName, version = 1)
            val migrated = openLatest(dbName)
            try {
                assertEquals(vehicles, migrated.vehicleDao().observeAll().first())
                freshDatabase().useDatabase { fresh ->
                    assertEquals(
                        tableSchemaSql(fresh.openHelper.readableDatabase),
                        tableSchemaSql(migrated.openHelper.readableDatabase),
                    )
                }
                val ledger = LedgerRepository(migrated)
                val entryId = UUID.randomUUID()
                assertEquals(
                    EntrySaveResult.Saved,
                    ledger.create(
                        entryId,
                        1_794_268_800_000L,
                        EntryInput(vehicles[0].id, "fuel", "2026-03-21", 250_000L, "30.5", "بنزین"),
                        kilometers = 12_000L,
                    ),
                )
                assertEquals(12_000L, ledger.observeCurrentMileage(vehicles[0].id).first())
                val reminders = ReminderRepository(migrated, ledger)
                val remId = UUID.randomUUID()
                assertEquals(
                    ReminderWriteResult.Saved,
                    reminders.create(vehicles[0].id, remId, ReminderInput("تعویض روغن", dueKilometers = 12_000L)),
                )
                assertEquals(remId.toString(), reminders.observeDue(vehicles[0].id).first().single().id)
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun reminderTriggersAndBackupRoundTripWorkOnSqlite() = runBlocking<Unit> {
        val exported = ByteArrayOutputStream()
        val canonicalVehicle = VehicleEntity(UUID.randomUUID().toString(), "car", "سمند LX", productionYear = 1400, productionYearCalendar = "solar_hijri")
        freshDatabase().useDatabase { sourceDb ->
            sourceDb.vehicleDao().insert(canonicalVehicle)
            val db = sourceDb.openHelper.writableDatabase
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("INSERT INTO reminders VALUES ('r-bad', '${canonicalVehicle.id}', 'Bad', NULL, NULL, 'active')")
            }
            assertThrows(SQLiteConstraintException::class.java) {
                db.execSQL("INSERT INTO reminders VALUES ('r-both', '${canonicalVehicle.id}', 'Bad', '2026-03-21', 100, 'active')")
            }
            val ledger = LedgerRepository(sourceDb)
            assertEquals(
                EntrySaveResult.Saved,
                ledger.createStandaloneReading(UUID.randomUUID(), OdometerInput(canonicalVehicle.id, "2026-03-21", 45_000L)),
            )
            val notifications = ReminderNotifications(context, { emptyList() })
            val backup = BackupRepository(sourceDb, notifications, {})
            assertEquals(BackupOutcome.Exported, backup.export { exported })
        }

        freshDatabase().useDatabase { targetDb ->
            val notifications = ReminderNotifications(context, { emptyList() })
            val backup = BackupRepository(targetDb, notifications, {})
            assertEquals(BackupOutcome.Restored, backup.restore { ByteArrayInputStream(exported.toByteArray()) })
            assertEquals(listOf(canonicalVehicle), targetDb.vehicleDao().observeAll().first())
            assertEquals(45_000L, targetDb.odometerDao().observeCurrentMileage(canonicalVehicle.id).first())
        }
    }

    private fun freshDatabase() = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
        .allowMainThreadQueries()
        .addCallback(reminderConstraints)
        .build()

    private fun openLatest(name: String) = Room.databaseBuilder(context, VehicleDatabase::class.java, name)
        .allowMainThreadQueries()
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .addCallback(reminderConstraints)
        .build()

    private fun createFixtureFromSchema(name: String, version: Int) {
        val schemaPath = "${VehicleDatabase::class.java.name}/$version.json"
        val file = listOf(File("schemas/$schemaPath"), File("app/schemas/$schemaPath")).first { it.exists() }
        val schema = JSONObject(file.readText()).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { database ->
            val tables = schema.getJSONArray("entities")
            for (i in 0 until tables.length()) {
                val table = tables.getJSONObject(i)
                val tableName = table.getString("tableName")
                database.execSQL(table.getString("createSql").replace("\${TABLE_NAME}", tableName))
                val indexes = table.optJSONArray("indices") ?: continue
                for (j in 0 until indexes.length()) {
                    database.execSQL(indexes.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", tableName))
                }
            }
            val setupQueries = schema.getJSONArray("setupQueries")
            for (i in 0 until setupQueries.length()) {
                database.execSQL(setupQueries.getString(i))
            }
            for (vehicle in vehicles) {
                database.execSQL(
                    "INSERT INTO vehicles (id, kind, displayLabel, catalogKey, productionYear, productionYearCalendar) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(vehicle.id, vehicle.kind, vehicle.displayLabel, vehicle.catalogKey, vehicle.productionYear, vehicle.productionYearCalendar),
                )
            }
            database.version = version
        }
    }

    private fun tableSchemaSql(database: SupportSQLiteDatabase): Map<String, String> {
        val whitespace = Regex("\\s+")
        return database.query(
            "SELECT name, sql FROM sqlite_master WHERE tbl_name IN ('vehicles', 'entries', 'odometer_readings', 'reminders') AND sql IS NOT NULL",
        ).use { cursor ->
            buildMap {
                while (cursor.moveToNext()) {
                    put(cursor.getString(0), cursor.getString(1).replace(whitespace, ""))
                }
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
}
