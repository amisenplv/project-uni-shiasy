package org.unishiasy.vehiclelog.data.local

import android.content.Context
import androidx.room.Room
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Migration4To5Test {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val vehicle = VehicleEntity(
        id = "vehicle-1",
        kind = "car",
        displayLabel = "پژو ۲۰۶",
        catalogKey = "peugeot-206",
        productionYear = 1399,
        productionYearCalendar = "solar_hijri",
    )

    @Test
    fun migratesPopulatedV4ToV5AndPreservesEntriesWithDefaults() = runBlocking {
        val dbName = "test-migration-4-5-${UUID.randomUUID()}.db"
        val dbFile = context.getDatabasePath(dbName)
        dbFile.parentFile?.mkdirs()

        try {
            createV4Database(dbName)

            // Open with Room at version 5 with MIGRATION_4_5
            val db = Room.databaseBuilder(context, VehicleDatabase::class.java, dbName)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(reminderConstraints)
                .allowMainThreadQueries()
                .build()

            try {
                val entries = db.ledgerDao().observeForVehicle(vehicle.id).first()
                assertEquals(2, entries.size)

                val v4Fuel = entries.first { it.id == "entry-fuel-v4" }
                assertEquals("fuel", v4Fuel.category)
                assertEquals("2026-11-20", v4Fuel.occurredOn)
                assertEquals(300_000L, v4Fuel.amountRials)
                assertEquals(30_000L, v4Fuel.fuelVolumeMl)
                assertEquals("بنزین سوپر", v4Fuel.notes)
                assertNull(v4Fuel.stationName)
                assertFalse(v4Fuel.isFullTank)

                val v4Oil = entries.first { it.id == "entry-oil-v4" }
                assertEquals("oil_change", v4Oil.category)
                assertEquals("2026-11-21", v4Oil.occurredOn)
                assertNull(v4Oil.stationName)
                assertFalse(v4Oil.isFullTank)

                // Insert a new v5 entry with stationName and fullTank = true
                val v5Entry = EntryEntity(
                    id = "entry-fuel-v5",
                    vehicleId = vehicle.id,
                    category = "fuel",
                    occurredOn = "2026-11-22",
                    createdAtEpochMillis = 1_794_268_900_000L,
                    notes = "بنزین معمولی",
                    amountRials = 350_000L,
                    fuelVolumeMl = 35_000L,
                    stationName = "جایگاه آزادی",
                    isFullTank = true,
                )
                db.ledgerDao().insert(v5Entry)

                val loadedV5 = db.ledgerDao().findById(v5Entry.id)
                assertEquals(v5Entry, loadedV5)
                assertEquals("جایگاه آزادی", loadedV5?.stationName)
                assertTrue(loadedV5?.isFullTank == true)
            } finally {
                db.close()
            }
        } finally {
            context.deleteDatabase(dbName)
        }
    }

    private fun createV4Database(name: String) {
        val schemaPath = "${VehicleDatabase::class.java.name}/4.json"
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

            database.execSQL(
                "INSERT INTO vehicles (id, kind, displayLabel, catalogKey, productionYear, productionYearCalendar) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(vehicle.id, vehicle.kind, vehicle.displayLabel, vehicle.catalogKey, vehicle.productionYear, vehicle.productionYearCalendar),
            )

            // Insert schema v4 entries (no station_name, no is_full_tank columns)
            database.execSQL(
                """
                INSERT INTO entries (id, vehicleId, category, occurredOn, createdAtEpochMillis, notes, amountRials, fuelVolumeMl)
                VALUES ('entry-fuel-v4', 'vehicle-1', 'fuel', '2026-11-20', 1794268800000, 'بنزین سوپر', 300000, 30000)
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO entries (id, vehicleId, category, occurredOn, createdAtEpochMillis, notes, amountRials, fuelVolumeMl)
                VALUES ('entry-oil-v4', 'vehicle-1', 'oil_change', '2026-11-21', 1794268850000, 'تعویض روغن', 1500000, NULL)
                """.trimIndent(),
            )

            database.version = 4
        }
    }
}
