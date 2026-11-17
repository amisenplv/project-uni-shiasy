package org.unishiasy.vehiclelog.data.backup

import android.app.NotificationManager
import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.reminders.NotificationChannels
import org.unishiasy.vehiclelog.reminders.ReminderDeliveryStore
import org.unishiasy.vehiclelog.reminders.ReminderNotifications

@RunWith(AndroidJUnit4::class)
class BackupRestoreTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val time = Instant.parse("2026-11-14T16:00:00Z")
    private val clock = Clock.fixed(time, ZoneOffset.UTC)
    private val vehicle = VehicleEntity(id(1), "car", "پژو LX", "catalog-key", 1400, "solar_hijri")
    private val fuel = EntryEntity(id(2), vehicle.id, "fuel", "2026-01-02", 42, "سوخت", Long.MAX_VALUE, 32000)
    private val service = EntryEntity(id(3), vehicle.id, "repair", "2026-01-03", 43, null, null, null)
    private val standalone = OdometerReadingEntity(id(4), vehicle.id, "2026-01-01", 100, null)
    private val linked = OdometerReadingEntity(id(5), vehicle.id, fuel.occurredOn, 110, fuel.id)
    private val mileage = ReminderEntity(id(6), vehicle.id, "سرویس", null, 150, "completed")
    private val date = ReminderEntity(id(7), vehicle.id, "روغن", "2026-11-13", null, "active")
    private val canceled = ReminderEntity(id(8), vehicle.id, "فیلتر", "2027-01-01", null, "canceled")
    private val snapshot = BackupSnapshot(time, listOf(vehicle), listOf(fuel, service),
        listOf(standalone, linked), listOf(mileage, date, canceled))

    @Test fun fullExportRestorePreservesAllRowsAndExcludesLaterWrites() = runBlocking<Unit> {
        fixture { source ->
            assertEquals(BackupOutcome.Restored, source.restore(snapshot))
            val beforeLaterWrite = source.export()
            assertEquals(snapshot, decode(beforeLaterWrite))
            source.database.ledgerDao().insert(service.copy(id = id(9)))
            assertEquals(3, decode(source.export()).entries.size)

            fixture { target ->
                val dateId = UUID.fromString(date.id)
                target.delivery.markDelivered(dateId)
                assertEquals(BackupOutcome.Restored, target.repository.restore { beforeLaterWrite.inputStream() })
                assertFalse(target.delivery.wasDelivered(dateId))
                assertEquals(1, target.reconciliations)
                assertArrayEquals(beforeLaterWrite, target.export())
                assertEquals(BackupOutcome.LedgerPopulated, target.restore(snapshot))
                assertEquals(1, target.reconciliations)
            }
        }
    }

    @Test fun emptySnapshotCanRestoreOnlyIntoAnEmptyLedger() = runBlocking<Unit> {
        fixture { target ->
            val empty = BackupSnapshot(time)
            assertEquals(empty, decode(target.export()))
            assertEquals(BackupOutcome.Restored, target.restore(empty))
            assertTrue(target.database.backupDao().isEmpty())
            target.database.vehicleDao().insert(vehicle)
            assertEquals(BackupOutcome.LedgerPopulated, target.restore(empty))
            assertEquals(1, target.reconciliations)
            assertEquals(listOf(vehicle), decode(target.export()).vehicles)
        }
    }

    @Test fun snapshotRemainsConsistentWhenWriterRunsDuringExport() = runBlocking<Unit> {
        fixture { source ->
            val sourceEmpty = BackupSnapshot(time)
            val writeStarted = CountDownLatch(1)
            val captureStarted = CountDownLatch(1)
            val heldClock = object : Clock() {
                override fun getZone(): ZoneId = ZoneOffset.UTC
                override fun withZone(zone: ZoneId): Clock = clock.withZone(zone)
                override fun instant(): Instant {
                    captureStarted.countDown()
                    check(writeStarted.await(10, TimeUnit.SECONDS))
                    return time
                }
            }
            val exporter = BackupRepository(source.database, source.notifications, {}, heldClock)
            val output = ByteArrayOutputStream()
            val export = async(Dispatchers.IO) { exporter.export { output } }
            val write = async(Dispatchers.IO) {
                check(captureStarted.await(10, TimeUnit.SECONDS))
                writeStarted.countDown()
                source.database.withTransaction {
                    source.database.vehicleDao().insert(vehicle)
                    source.database.ledgerDao().insert(fuel)
                }
            }
            assertEquals(BackupOutcome.Exported, export.await())
            write.await()
            assertEquals(sourceEmpty, decode(output.toByteArray()))
            assertEquals(listOf(fuel), decode(source.export()).entries)
        }
    }

    @Test fun inTransactionRecheckRejectsLedgerPopulatedDuringFileRead() = runBlocking<Unit> {
        fixture { target ->
            val old = UUID.fromString(date.id)
            target.delivery.markDelivered(old)
            val input = encode(snapshot)
            val anotherVehicle = vehicle.copy(id = id(9))
            val result = target.repository.restore {
                runBlocking { target.database.vehicleDao().insert(anotherVehicle) }
                input.inputStream()
            }
            assertEquals(BackupOutcome.LedgerPopulated, result)
            assertEquals(BackupSnapshot(time, vehicles = listOf(anotherVehicle)), decode(target.export()))
            assertTrue(target.delivery.wasDelivered(old))
            assertEquals(0, target.reconciliations)
        }
    }

    @Test fun failureOnLastTableRollsBackEarlierInsertsAndPreservesNotificationState() = runBlocking<Unit> {
        fixture { target ->
            val id = UUID.fromString(date.id)
            target.delivery.markDelivered(id)
            target.database.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_restore BEFORE INSERT ON reminders BEGIN SELECT RAISE(ABORT, 'test failure'); END",
            )
            assertEquals(BackupOutcome.StorageFailure, target.restore(snapshot))
            assertEquals(BackupSnapshot(time), decode(target.export()))
            assertTrue(target.delivery.wasDelivered(id))
            assertEquals(0, target.reconciliations)
            target.database.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_restore")
            assertEquals(BackupOutcome.Restored, target.restore(snapshot))
            assertFalse(target.delivery.wasDelivered(id))
            assertEquals(1, target.reconciliations)
        }
    }

    @Test fun invalidOversizedAndUnreadableDocumentsLeaveLedgerAndMarkersIntact() = runBlocking<Unit> {
        fixture { target ->
            val id = UUID.fromString(date.id)
            target.delivery.markDelivered(id)
            assertEquals(BackupOutcome.InvalidData, target.repository.restore { "{".byteInputStream() })
            val version = encode(snapshot).decodeToString().replace("\"formatVersion\":1", "\"formatVersion\":2")
            assertEquals(BackupOutcome.UnsupportedFormat, target.repository.restore { version.byteInputStream() })
            assertEquals(BackupOutcome.TooLarge, target.repository.restore {
                ByteArray(BackupCodec.MAX_BYTES + 1).inputStream()
            })
            assertEquals(BackupOutcome.Canceled, target.repository.restore(null))
            assertEquals(BackupOutcome.FileUnavailable, target.repository.restore { null })
            assertEquals(BackupOutcome.FileUnavailable, target.repository.restore {
                object : InputStream() { override fun read(): Int = throw IOException() }
            })
            assertEquals(BackupOutcome.FileUnavailable, target.repository.restore {
                object : java.io.ByteArrayInputStream(encode(snapshot)) { override fun close() = throw IOException() }
            })
            assertTrue(target.database.backupDao().isEmpty())
            assertTrue(target.delivery.wasDelivered(id))
            assertEquals(0, target.reconciliations)
        }
    }

    @Test fun exportFailureIncludesWriteAndCloseAndPreservesLedger() = runBlocking<Unit> {
        fixture { source ->
            assertEquals(BackupOutcome.Restored, source.restore(snapshot))
            assertEquals(BackupOutcome.Canceled, source.repository.export(null))
            assertEquals(BackupOutcome.FileUnavailable, source.repository.export { null })
            assertEquals(BackupOutcome.FileUnavailable, source.repository.export {
                object : OutputStream() { override fun write(value: Int) = throw IOException() }
            })
            assertEquals(BackupOutcome.FileUnavailable, source.repository.export {
                object : ByteArrayOutputStream() { override fun close() = throw IOException() }
            })
            assertEquals(snapshot, decode(source.export()))
            repeat(BackupCodec.MAX_VEHICLES) { index ->
                source.database.vehicleDao().insert(vehicle.copy(id = id(20L + index)))
            }
            assertEquals(BackupOutcome.TooLarge, source.repository.export { error("Destination opened before encoding") })
        }
    }

    @Test fun successfulRestoreRemovesStaleActiveNotificationBeforeScheduling() = runBlocking<Unit> {
        fixture { target ->
            val channel = target.channels
            org.junit.Assume.assumeTrue(channel.available())
            // Use a unique tag and remove it even if the assertions fail.
            val stale = UUID.randomUUID()
            val manager = context.getSystemService(NotificationManager::class.java)
            try {
                assertTrue(channel.post(stale.toString(), "آزمون بازیابی"))
                await { manager.activeNotifications.any { it.tag == "${target.tagPrefix}$stale" } }
                val old = UUID.fromString(date.id)
                target.delivery.markDelivered(old)
                target.verifyBeforeScheduling = {
                    assertFalse(target.delivery.wasDelivered(old))
                    await { manager.activeNotifications.none { it.tag == "${target.tagPrefix}$stale" } }
                }
                assertEquals(BackupOutcome.Restored, target.restore(snapshot))
                assertEquals(1, target.reconciliations)
            } finally {
                channel.cancel(stale.toString())
            }
        }
    }

    private fun await(condition: () -> Boolean) {
        repeat(40) {
            if (condition()) return
            Thread.sleep(50)
        }
        assertTrue(condition())
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val fixture = Fixture()
        try {
            block(fixture)
        } finally {
            fixture.database.close()
            fixture.directory.deleteRecursively()
            context.deleteSharedPreferences(fixture.preferenceName)
        }
    }

    private inner class Fixture {
        val database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .addCallback(reminderConstraints).build()
        val preferenceName = "backup-test-${UUID.randomUUID()}"
        val directory = File(context.noBackupFilesDir, preferenceName)
        val delivery = ReminderDeliveryStore(directory)
        val tagPrefix = "$preferenceName:"
        val channels = NotificationChannels(context, tagPrefix)
        var reconciliations = 0
        var verifyBeforeScheduling: () -> Unit = {}
        val notifications = ReminderNotifications(
            context, database.reminderDao()::activeDateReminders, clock, preferenceName,
            deliveryDirectory = directory, channels = channels,
        )
        val repository = BackupRepository(database, notifications, {
            verifyBeforeScheduling()
            reconciliations++
        }, clock)

        suspend fun restore(snapshot: BackupSnapshot) = repository.restore { encode(snapshot).inputStream() }

        suspend fun export(): ByteArray {
            val output = ByteArrayOutputStream()
            assertEquals(BackupOutcome.Exported, repository.export { output })
            return output.toByteArray()
        }
    }

    private fun encode(value: BackupSnapshot) = (BackupCodec.encode(value) as BackupResult.Success).value
    private fun decode(value: ByteArray) = (BackupCodec.decode(value) as BackupResult.Success).value
    private fun id(number: Long) = UUID(0, number).toString()
}
