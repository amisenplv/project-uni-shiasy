package org.unishiasy.vehiclelog.data.local

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VehicleDaoTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: VehicleDatabase
    private val vehicle = VehicleEntity(id = "vehicle-b", kind = "car", displayLabel = "پژو ۲۰۶")

    @Before
    fun openDatabase() {
        database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun insertsAndReadsVehiclesInIdOrder() = runBlocking<Unit> {
        val dao = database.vehicleDao()
        val other = vehicle.copy(
            id = "vehicle-a",
            catalogKey = "peugeot-206",
            productionYear = 1400,
            productionYearCalendar = "solar_hijri",
        )
        dao.insert(vehicle)
        dao.insert(other)

        assertEquals(vehicle, dao.findById(vehicle.id))
        assertEquals(other, dao.findById(other.id))
        assertEquals(listOf(other, vehicle), dao.observeAll().first())
    }

    @Test
    fun duplicatePrimaryKeyIsRejectedWithoutReplacingTheRow() = runBlocking<Unit> {
        val dao = database.vehicleDao()
        dao.insert(vehicle)

        try {
            dao.insert(vehicle.copy(displayLabel = "پژو ۲۰۷"))
            fail("Expected a duplicate primary key to be rejected")
        } catch (_: SQLiteConstraintException) {
            // The original row must still be present after ABORT.
        }

        assertEquals(vehicle, dao.findById(vehicle.id))
    }

    @Test
    fun updateReportsAffectedRowsAndDoesNotInsertMissingIds() = runBlocking<Unit> {
        val dao = database.vehicleDao()
        val edited = vehicle.copy(displayLabel = "پژو ۲۰۷")
        val missing = edited.copy(id = "missing")
        dao.insert(vehicle)

        assertEquals(1, dao.update(edited))
        assertEquals(edited, dao.findById(vehicle.id))
        assertEquals(0, dao.update(missing))
        assertNull(dao.findById(missing.id))
    }

    @Test
    fun fileBackedDatabaseRetainsVehicleAfterReopen() = runBlocking<Unit> {
        val name = "vehicle-dao-test-${UUID.randomUUID()}.db"
        try {
            val first = Room.databaseBuilder(context, VehicleDatabase::class.java, name).build()
            try {
                first.vehicleDao().insert(vehicle)
            } finally {
                first.close()
            }

            val reopened = Room.databaseBuilder(context, VehicleDatabase::class.java, name).build()
            try {
                assertEquals(vehicle, reopened.vehicleDao().findById(vehicle.id))
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
