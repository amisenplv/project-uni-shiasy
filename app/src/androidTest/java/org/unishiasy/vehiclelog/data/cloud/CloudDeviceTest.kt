package org.unishiasy.vehiclelog.data.cloud

import android.content.ContextWrapper
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.BuildConfig
import org.unishiasy.vehiclelog.data.backup.BackupCodec
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.data.backup.BackupResult
import org.unishiasy.vehiclelog.data.backup.BackupSnapshot
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.reminders.ReminderNotifications

@RunWith(AndroidJUnit4::class)
class CloudDeviceTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val password = "synthetic local Android fixture"
    private val api = LocalCloudApi()
    private val time = Instant.parse("2026-11-15T12:00:00Z")
    private val vehicle = VehicleEntity("00000000-0000-0000-0000-000000000001", "car", "آزمون ساختگی 🚘")
    private val entry = EntryEntity("00000000-0000-0000-0000-000000000002", vehicle.id, "fuel",
        "2026-11-14", 42, "آزمون", Long.MAX_VALUE, 1500)

    @Before fun requireLocalFixture() {
        assumeTrue(BuildConfig.LOCAL_CLOUD_ENABLED &&
            InstrumentationRegistry.getArguments().getString("localCloudFixture") == "true")
    }

    @Test fun localRoundTripUsesRoomAndKeystoreAndRevokedSessionKeepsLedger() = runBlocking<Unit> {
        val username = username("normal")
        val db = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).build()
        val store = store("normal")
        val capture = BackupRepository(db, ReminderNotifications(context, { emptyList() }), {}, Clock.fixed(time, ZoneOffset.UTC))
        fun repo() = CloudRepository(api, store, capture::captureSnapshot, capture::bindIfEmpty)
        try {
            db.vehicleDao().insert(vehicle)
            db.ledgerDao().insert(entry)
            assertNull(repo().status().username)
            assertEquals(entry, db.backupDao().entries(2).single())

            assertTrue(cloudCode.matches(checkNotNull(repo().signIn(username, password, invite()))))
            val persisted = checkNotNull(store.read().session)
            val envelope = File(directory("normal"), "local-cloud-v1").readBytes().decodeToString()
            assertFalse(envelope.contains(persisted.token))
            assertFalse(envelope.contains(password))

            repo().bind()
            assertEquals(username, repo().status().username)
            val expected = (capture.captureSnapshot() as BackupResult.Success).value
            assertNull(repo().startUpload())
            val ack = checkNotNull(repo().status().ack)
            assertEquals(expected.sha256(), ack.digest)
            assertEquals(expected.size, ack.length)
            assertEquals(ack, api.metadata(persisted).snapshot)

            api.logout(persisted)
            expect(CloudIssue.Expired) { repo().review() }
            assertNull(store.read().session)
            assertEquals(entry, db.backupDao().entries(2).single())
            assertNull(repo().signIn(username, password, null))
            assertEquals(ack, repo().status().ack)
            repo().signOut()
            assertNull(repo().status().username)
            assertEquals(entry, db.backupDao().entries(2).single())
        } finally {
            db.close()
        }
    }

    @Test fun stageUploadUntilHarnessKillsProcess() = runBlocking<Unit> {
        val store = store("death")
        val db = Room.databaseBuilder(context, VehicleDatabase::class.java, "m53-test-death.db").build()
        try {
            db.vehicleDao().insert(vehicle)
            db.ledgerDao().insert(entry)
            val capture = capture(db)
            val repo = CloudRepository(api, store, capture::captureSnapshot, capture::bindIfEmpty)
            repo.signIn(username("death"), password, invite())
            repo.bind()
            repo.startUpload()
            // A harness deliberately holds the PUT reply after PostgreSQL commits.
            error("The process-death fixture should be stopped before acknowledgement.")
        } finally {
            db.close()
        }
    }

    @Test fun readPendingAfterActualProcessDeathWithoutSending() = runBlocking<Unit> {
        val store = store("death")
        val repo = processRepo(store)
        assertEquals(CloudRetry.Pending, repo.status().retry)
        val record = store.read()
        assertNull(record.ack)
        val pending = checkNotNull(record.pending)
        assertArrayEquals(originalBytes(), pending.bytes)
        assertTrue(cloudUuid.matches(pending.attempt))
        val opaque = File(directory("death"), "local-cloud-v1").readBytes().decodeToString()
        assertFalse(opaque.contains(checkNotNull(record.session).token))
        assertFalse(opaque.contains(pending.attempt))
    }

    @Test fun explicitRetryAfterProcessDeathConfirmsOriginalDespiteLaterEdits() = runBlocking<Unit> {
        val store = store("death")
        val pending = checkNotNull(store.read().pending)
        val db = Room.databaseBuilder(context, VehicleDatabase::class.java, "m53-test-death.db").build()
        try {
            db.ledgerDao().insert(entry.copy(id = UUID.randomUUID().toString()))
            val capture = capture(db)
            val changed = (capture.captureSnapshot() as BackupResult.Success).value
            assertFalse(changed.contentEquals(pending.bytes))
            val repo = CloudRepository(api, store, capture::captureSnapshot, capture::bindIfEmpty)
            assertEquals(CloudRetry.Pending, repo.status().retry)
            repo.retry()
            val ack = checkNotNull(processRepo(store).status().ack)
            assertEquals(pending.attempt, ack.attempt)
            assertEquals(pending.bytes.sha256(), ack.digest)
            assertEquals(pending.bytes.size, ack.length)
            assertFalse(ack.digest == changed.sha256())
            assertNull(store.read().pending)
            assertEquals(ack, api.metadata(checkNotNull(store.read().session)).snapshot)
        } finally {
            db.close()
        }
    }

    @Test fun confirmedMetadataSurvivesAnotherProcessRestart() = runBlocking<Unit> {
        val record = store("death").read()
        assertNull(record.pending)
        assertNotNull(record.session)
        assertEquals(originalBytes().sha256(), checkNotNull(processRepo(store("death")).status().ack).digest)
    }

    @Test fun disconnectBeforeServerReceivesUploadIsUnconfirmed() = runBlocking<Unit> {
        disconnect(committed = false)
    }

    @Test fun disconnectAfterServerCommitsUploadIsUnconfirmed() = runBlocking<Unit> {
        disconnect(committed = true)
    }

    private suspend fun disconnect(committed: Boolean) {
        val store = store("disconnect-${UUID.randomUUID()}")
        val repo = processRepo(store)
        repo.signIn(username("wire"), password, invite())
        repo.bind()
        expect(CloudIssue.Unconfirmed) { repo.startUpload() }
        assertEquals(CloudRetry.Pending, repo.status().retry)
        assertNull(repo.status().ack)
        val record = store.read()
        val pending = checkNotNull(record.pending)
        assertArrayEquals(originalBytes(), pending.bytes)
        val remote = api.metadata(checkNotNull(record.session)).snapshot
        assertEquals(if (committed) pending.attempt else null, remote?.attempt)
    }

    private fun capture(db: VehicleDatabase) =
        BackupRepository(db, ReminderNotifications(context, { emptyList() }), {}, Clock.fixed(time, ZoneOffset.UTC))

    private fun processRepo(store: CloudStore, bytes: ByteArray = originalBytes()) =
        CloudRepository(api, store, { BackupResult.Success(bytes) }, { false })

    private fun originalBytes() = encoded(BackupSnapshot(time, listOf(vehicle), listOf(entry)))
    private fun encoded(snapshot: BackupSnapshot) = (BackupCodec.encode(snapshot) as BackupResult.Success).value
    private fun invite() = checkNotNull(InstrumentationRegistry.getArguments().getString("invite"))
    private fun username(kind: String) = "m53${kind}_" + UUID.randomUUID().toString().take(8)
    private fun directory(name: String) = File(context.noBackupFilesDir, "m53-test-$name").apply { mkdirs() }
    private fun store(name: String) = KeystoreCloudStore(object : ContextWrapper(context) {
        override fun getNoBackupFilesDir() = directory(name)
    })

    private suspend fun expect(issue: CloudIssue, action: suspend () -> Unit) {
        assertEquals(issue, (runCatching { action() }.exceptionOrNull() as? CloudFailure)?.issue)
    }
}
