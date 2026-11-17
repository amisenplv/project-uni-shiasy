package org.unishiasy.vehiclelog.reminders

import android.app.NotificationManager
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.ListenableWorker.Result
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.ReminderInput
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.ReminderWriteResult
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints

@RunWith(AndroidJUnit4::class)
class ReminderWorkerTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val channels = NotificationChannels(context)

    @Test
    fun delayedReconciliationAndRestartSkipPreviouslyDeliveredAndTerminalReminders() = runBlocking<Unit> {
        fixture { fixture ->
            assertTrue(fixture.notifications.setOptedIn(true))
            val due = fixture.create("2026-03-21")
            val pending = fixture.create("2026-04-30")
            val completed = fixture.create("2026-03-21")
            val canceled = fixture.create("2026-03-21")
            val mileage = fixture.create(null)
            assertEquals(ReminderWriteResult.Saved, fixture.repository.complete("vehicle", completed))
            assertEquals(ReminderWriteResult.Saved, fixture.repository.cancel("vehicle", canceled))

            fixture.now = Instant.parse("2026-03-20T20:29:59Z")
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            assertFalse(fixture.delivery.wasDelivered(due))
            assertFalse(due.toString() in channels.activeReminderIds())

            fixture.now = Instant.parse("2026-03-24T10:00:00Z")
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            awaitPosted(due)
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            assertEquals(1, manager.activeNotifications.count { it.tag == "date-reminder:$due" })
            for (id in listOf(pending, completed, canceled, mileage)) {
                assertFalse(fixture.delivery.wasDelivered(id))
                assertFalse(id.toString() in channels.activeReminderIds())
            }

            val restarted = fixture.newService()
            assertTrue(restarted.optedIn())
            channels.cancel(due.toString())
            awaitRemoved(due)
            assertEquals(Result.success(), reconcileReminders(restarted))
            assertFalse(due.toString() in channels.activeReminderIds())
            assertTrue(fixture.delivery.wasDelivered(due))

            val next = fixture.create("2026-03-22")
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            awaitPosted(next)
            assertEquals(ReminderWriteResult.Saved, fixture.repository.cancel("vehicle", next))
            awaitRemoved(next)
            assertEquals(Result.success(), reconcileReminders(restarted))
            assertFalse(next.toString() in channels.activeReminderIds())

            val active = fixture.create("2026-03-22")
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            awaitPosted(active)
            // Simulate a committed lifecycle write before notification cleanup can run.
            assertEquals(1, fixture.database.reminderDao().finish("vehicle", active.toString(), "completed"))
            assertEquals(Result.success(), reconcileReminders(restarted))
            awaitRemoved(active)
        }

    }

    @Test
    fun systemNotificationSettingControlsDelivery() = runBlocking<Unit> {
        val expectation = InstrumentationRegistry.getArguments().getString("notificationPermissionExpectation")
        assumeTrue(expectation == "denied" || expectation == "granted")
        fixture { fixture ->
            val id = fixture.create("2026-03-21")
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            assertFalse(id.toString() in channels.activeReminderIds())
            assertTrue(fixture.notifications.setOptedIn(true))
            assertEquals(expectation == "granted", fixture.notifications.systemAllowsNotifications())
            assertEquals(Result.success(), reconcileReminders(fixture.notifications))
            if (expectation == "granted") {
                awaitPosted(id)
                assertEquals(
                    NotificationManager.IMPORTANCE_DEFAULT,
                    manager.getNotificationChannel(NotificationChannels.DATE_CHANNEL).importance,
                )
            } else {
                assertFalse(id.toString() in channels.activeReminderIds())
            }
            assertEquals(expectation == "granted", fixture.delivery.wasDelivered(id))
            assertEquals(id.toString(), fixture.repository.observeDue("vehicle").first().single().id)
            assertTrue(fixture.notifications.setOptedIn(false))
            awaitRemoved(id)
        }
    }

    @Test
    fun deliveryMarkerSurvivesProcessRestart() = runBlocking<Unit> {
        val args = InstrumentationRegistry.getArguments()
        val phase = args.getString("notificationRestartPhase")
        val fixture = args.getString("notificationRestartFixture").orEmpty()
        assumeTrue(phase == "write" || phase == "read")
        require(fixture.matches(Regex("m43-restart-[a-z0-9]+")))
        val id = UUID.nameUUIDFromBytes(fixture.toByteArray())
        val directory = File(context.noBackupFilesDir, fixture)
        val row = ReminderEntity(id.toString(), "test-vehicle", "یادآور آزمون", "2026-03-21", null, "active")
        val service = ReminderNotifications(
            context, { listOf(row) }, Clock.fixed(Instant.parse("2026-03-21T00:00:00Z"), ZoneOffset.UTC),
            fixture, deliveryDirectory = directory,
        )
        try {
            assertTrue(service.systemAllowsNotifications())
            if (phase == "write") {
                assertTrue(service.setOptedIn(true))
                assertEquals(Result.success(), reconcileReminders(service))
                awaitPosted(id)
                channels.cancel(id.toString())
                awaitRemoved(id)
            } else {
                assertTrue(service.optedIn())
                assertTrue(ReminderDeliveryStore(directory).wasDelivered(id))
                assertEquals(Result.success(), reconcileReminders(service))
                assertFalse(id.toString() in channels.activeReminderIds())
            }
        } finally {
            if (phase == "read") {
                channels.cancel(id.toString())
                directory.deleteRecursively()
                context.deleteSharedPreferences(fixture)
            }
        }
    }

    private suspend fun fixture(block: suspend (Fixture) -> Unit) {
        val fixture = Fixture()
        try {
            fixture.database.vehicleDao().insert(VehicleEntity("vehicle", "car", "خودروی آزمون"))
            block(fixture)
        } finally {
            fixture.ids.forEach { channels.cancel(it.toString()) }
            fixture.database.close()
            fixture.directory.deleteRecursively()
            context.deleteSharedPreferences(fixture.preferenceName)
        }
    }

    private inner class Fixture {
        val database = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java)
            .addCallback(reminderConstraints)
            .build()
        val ids = mutableListOf<UUID>()
        val preferenceName = "reminder-test-${UUID.randomUUID()}"
        val directory = File(context.noBackupFilesDir, preferenceName)
        val delivery = ReminderDeliveryStore(directory)
        var now: Instant = Instant.parse("2026-03-20T20:30:00Z")
        private val clock = object : Clock() {
            override fun instant(): Instant = now
            override fun getZone() = ZoneOffset.UTC
            override fun withZone(zone: java.time.ZoneId): Clock = Clock.fixed(now, zone)
        }
        val repository: ReminderRepository = ReminderRepository(
            database,
            LedgerRepository(database),
            clock,
            finishWithCleanup = { id, finish -> notifications.finishAndCancel(id, finish) },
        )
        val notifications: ReminderNotifications = newService()

        fun newService() = ReminderNotifications(
            context,
            repository::activeDateReminders,
            clock,
            preferenceName,
            deliveryDirectory = directory,
        )

        suspend fun create(date: String?): UUID {
            val id = UUID.randomUUID()
            ids += id
            val input = ReminderInput("یادآور آزمون", dueOn = date, dueKilometers = if (date == null) 0 else null)
            assertEquals(ReminderWriteResult.Saved, repository.create("vehicle", id, input))
            return id
        }
    }

    private fun awaitPosted(id: UUID) {
        awaitNotification(id, present = true)
    }

    private fun awaitRemoved(id: UUID) {
        awaitNotification(id, present = false)
    }

    private fun awaitNotification(id: UUID, present: Boolean) {
        for (attempt in 1..40) {
            if ((id.toString() in channels.activeReminderIds()) == present) return
            Thread.sleep(50)
        }
        assertEquals("Notification presence", present, id.toString() in channels.activeReminderIds())
    }

}
