package org.unishiasy.vehiclelog.data.cloud

import android.content.ContextWrapper
import androidx.room.Room
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.time.Instant
import java.math.BigInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.unishiasy.vehiclelog.BuildConfig
import org.unishiasy.vehiclelog.MainActivity
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.VehicleLedgerApplication
import org.unishiasy.vehiclelog.data.EntryInput
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.VehicleInput
import org.unishiasy.vehiclelog.data.VehicleSaveResult
import org.unishiasy.vehiclelog.data.backup.BackupCodec
import org.unishiasy.vehiclelog.data.backup.BackupOutcome
import org.unishiasy.vehiclelog.data.backup.BackupRepository
import org.unishiasy.vehiclelog.data.backup.BackupResult
import org.unishiasy.vehiclelog.data.backup.BackupSnapshot
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.data.local.reminderConstraints
import org.unishiasy.vehiclelog.reminders.ReminderDeliveryStore
import org.unishiasy.vehiclelog.reminders.ReminderNotifications

private val registry get() = InstrumentationRegistry.getInstrumentation()
private val context get() = registry.targetContext
private val app get() = context.applicationContext as VehicleLedgerApplication
private val arguments get() = InstrumentationRegistry.getArguments()
private val account get() = checkNotNull(arguments.getString("account"))
private const val password = "synthetic recovery fixture passphrase"

private fun requireFixture() {
    assumeTrue(BuildConfig.LOCAL_CLOUD_ENABLED && arguments.getString("localCloudFixture") == "true")
}

private val vehicle = VehicleEntity(id(1), "car", "پژو LX", "catalog-key", 1400, "solar_hijri")
private val fuel = EntryEntity(id(2), vehicle.id, "fuel", "2026-01-02", 42, "سوخت", Long.MAX_VALUE, 32000)
private val service = EntryEntity(id(3), vehicle.id, "repair", "2026-01-03", 43, null, null, null)
private val standalone = OdometerReadingEntity(id(4), vehicle.id, "2026-01-01", 100, null)
private val linked = OdometerReadingEntity(id(5), vehicle.id, fuel.occurredOn, 110, fuel.id)
private val due = ReminderEntity(id(6), vehicle.id, "روغن", "2026-11-13", null, "active")
private val expected = BackupSnapshot(Instant.parse("2026-11-14T16:00:00Z"), listOf(vehicle), listOf(fuel, service),
    listOf(standalone, linked), listOf(due,
        ReminderEntity(id(7), vehicle.id, "سرویس", null, 150, "completed"),
        ReminderEntity(id(8), vehicle.id, "فیلتر", "2027-01-01", null, "canceled")))
private fun id(number: Long) = UUID(0, number).toString()
private fun encode(snapshot: BackupSnapshot) = (BackupCodec.encode(snapshot) as BackupResult.Success).value
private suspend fun capture(repository: BackupRepository = app.container.backupRepository): BackupSnapshot =
    BackupCodec.decode((repository.captureSnapshot() as BackupResult.Success).value).let { (it as BackupResult.Success).value }

private suspend fun expect(issue: CloudIssue, work: suspend () -> Unit) {
    try {
        work()
        fail("Expected $issue")
    } catch (failure: CloudFailure) {
        assertEquals(issue, failure.issue)
    }
}

@RunWith(AndroidJUnit4::class)
class CloudRecoveryDeviceTest {
    @Before fun requireLocalFixture() = requireFixture()

    @Test fun uploadOnInstallationAThenAddLaterRecord() = runBlocking<Unit> {
        val backup = app.container.backupRepository
        val repo = checkNotNull(app.container.cloudRepository)
        assertTrue(backup.isEmpty())
        assertNull(repo.status().username)
        assertNotNull(repo.signIn(account, password, checkNotNull(arguments.getString("invite"))))
        repo.bind()
        expect(CloudIssue.NoSnapshot) { repo.selectRestore() }
        assertEquals(BackupOutcome.Restored, backup.restore { encode(expected).inputStream() })
        val before = capture()
        assertEquals(expected, before.copy(exportedAt = expected.exportedAt))
        assertNull(repo.startUpload())
        val ack = checkNotNull(repo.status().ack)
        val session = checkNotNull(KeystoreCloudStore(context).read().session)
        val downloaded = LocalCloudApi().download(session, ack)
        val accepted = (BackupCodec.decode(downloaded) as BackupResult.Success).value
        assertEquals(ack.digest, downloaded.sha256())
        assertEquals(expected, accepted.copy(exportedAt = expected.exportedAt))
        assertTrue(app.container.vehicleRepository.create(VehicleInput("ثبت بعد از ارسال", null, null, null))
            is VehicleSaveResult.Created)
        assertEquals(2, capture().vehicles.size)
    }

    @Test fun startDownloadOnEmptyBAndWaitForHarnessTermination() = runBlocking<Unit> {
        val backup = app.container.backupRepository
        val repo = checkNotNull(app.container.cloudRepository)
        assertTrue(backup.isEmpty())
        assertNull(repo.status().username)
        repo.signIn(account, password, null)
        repo.bind()
        repo.restore(repo.selectRestore())
        error("Harness must stop this process before it receives downloaded bytes.")
    }

    @Test fun afterRestartRestoredLedgerIsExactAndNoRetryRuns() = runBlocking<Unit> {
        val actual = capture()
        assertEquals(expected, actual.copy(exportedAt = expected.exportedAt))
        val appRecord = KeystoreCloudStore(context).read()
        assertNotNull(appRecord.restoredRevision)
        assertNull(appRecord.ack)
        assertNull(appRecord.pending)
        assertEquals(110L, app.container.ledgerRepository.observeCurrentMileage(vehicle.id).first())
        val month = app.container.ledgerRepository.observeMonthlyReport(vehicle.id).first().months.single()
        assertEquals(BigInteger.valueOf(Long.MAX_VALUE), month.totals.totalRials)
        expect(CloudIssue.PopulatedLedger) { checkNotNull(app.container.cloudRepository).selectRestore() }
    }

    @Test fun logAnEntryOfflineInRecoveredLedger() = runBlocking<Unit> {
        val ledger = app.container.ledgerRepository
        val new = UUID.fromString(id(60))
        assertEquals(EntrySaveResult.Saved, ledger.create(new, 70,
            EntryInput(vehicle.id, "repair", "2026-03-01", 7)))
        assertEquals(7L, checkNotNull(ledger.findById(new)).amountRials)
    }

    @Test fun holdAfterCommitBeforeRecordingCloudReceipt() = runBlocking<Unit> {
        val db = interruptedDatabase()
        val store = interruptedStore()
        val notifications = ReminderNotifications(context, { emptyList() },
            deliveryDirectory = File(interruptedDirectory(), "markers"))
        val backup = BackupRepository(db, notifications, {
            // Inject interruption after Room commits and the existing marker reset finishes.
            File(context.filesDir, "m54-commit-ready").writeText("ready")
            check(CountDownLatch(1).await(30, TimeUnit.SECONDS))
        })
        try {
            assertTrue(backup.isEmpty())
            store.write(KeystoreCloudStore(context).read().copy(restoredRevision = null))
            val repo = CloudRepository(LocalCloudApi(), store, backup::captureSnapshot, backup::bindIfEmpty, backup = backup)
            repo.restore(repo.selectRestore())
            error("Harness must stop this process after commit and before receipt storage.")
        } finally {
            db.close()
        }
    }

    @Test fun committedRowsSurviveWithoutCloudReceiptAndBlockRepeatRestore() = runBlocking<Unit> {
        val db = interruptedDatabase()
        try {
            val backup = BackupRepository(db, ReminderNotifications(context, { emptyList() }), {})
            assertEquals(expected, capture(backup).copy(exportedAt = expected.exportedAt))
            val store = interruptedStore()
            assertNull(store.read().restoredRevision)
            val repo = CloudRepository(LocalCloudApi(), store, backup::captureSnapshot, backup::bindIfEmpty, backup = backup)
            expect(CloudIssue.PopulatedLedger) { repo.selectRestore() }
        } finally {
            db.close()
        }
    }

    private fun interruptedDatabase() = Room.databaseBuilder(
        context, VehicleDatabase::class.java, "m54-interrupted-receipt.db",
    ).addCallback(reminderConstraints).build()

    private fun interruptedDirectory() = File(context.noBackupFilesDir, "m54-interrupted-receipt").apply { mkdirs() }

    private fun interruptedStore() = KeystoreCloudStore(object : ContextWrapper(context) {
        override fun getNoBackupFilesDir() = interruptedDirectory()
    })

    @Test fun downloadFaultLeavesRoomAndNotificationMarkersUntouched() = runBlocking<Unit> {
        val issue = CloudIssue.valueOf(checkNotNull(arguments.getString("expectedIssue")))
        fixture { target ->
            target.store.write(KeystoreCloudStore(context).read().copy(restoredRevision = null))
            val selection = target.repo.selectRestore()
            expect(issue) { target.repo.restore(selection) }
            target.assertEmptyAndUnreconciled()
        }
    }

    @Test fun finalAuthorizationAndSqliteFailuresDoNotChangeExistingDataOrMarkers() = runBlocking<Unit> {
        val http = LocalCloudApi()
        fixture { target ->
            val owner = target.session()
            var metadataRecheck = false
            val revokedAtRecheck = object : CloudApi by http {
                override suspend fun download(session: CloudSession, selected: CloudAck): ByteArray =
                    http.download(session, selected).also { http.logout(session); metadataRecheck = true }
            }
            val revoked = target.repo(revokedAtRecheck)
            val selection = revoked.selectRestore()
            expect(CloudIssue.Expired) { revoked.restore(selection) }
            assertTrue(metadataRecheck)
            assertNull(target.store.read().session)
            target.assertEmptyAndUnreconciled()

            val current = target.session()
            assertNotEquals(owner.token, current.token)
            expect(CloudIssue.RestoreChanged) { target.repo.restore(selection) }
            val changedAccount = target.repo(object : CloudApi by http {
                override suspend fun download(session: CloudSession, selected: CloudAck): ByteArray {
                    error("A different account must fail before download.")
                }
            })
            val stale = changedAccount.selectRestore()
            val other = target.store.read().copy(session = CloudSession(id(20), current.token, current.expires, "other"))
            target.store.write(other)
            expect(CloudIssue.DifferentAccount) { changedAccount.restore(stale) }
            target.store.write(other.copy(session = current))

            val unsupportedBytes = encode(expected).decodeToString().replace(
                "\"formatVersion\":1", "\"formatVersion\":2",
            ).encodeToByteArray()
            val unsupported = object : CloudApi by http {
                override suspend fun metadata(session: CloudSession): CloudSlot = http.metadata(session).let {
                    it.copy(snapshot = checkNotNull(it.snapshot).copy(
                        length = unsupportedBytes.size, digest = unsupportedBytes.sha256(),
                    ))
                }
                override suspend fun download(session: CloudSession, selected: CloudAck) = unsupportedBytes.copyOf()
            }
            val invalid = target.repo(unsupported)
            expect(CloudIssue.RestoreInvalid) { invalid.restore(invalid.selectRestore()) }
            target.assertEmptyAndUnreconciled()

            val oldRevision = target.repo.selectRestore()
            val initialReplacement = expected.copy(exportedAt = expected.exportedAt.plusSeconds(1))
            http.upload(current, CloudPending(UUID.randomUUID().toString(), oldRevision.snapshot.revision,
                encode(initialReplacement)))
            expect(CloudIssue.RestoreChanged) { target.repo.restore(oldRevision) }
            target.assertEmptyAndUnreconciled()

            val replacement = object : CloudApi by http {
                override suspend fun download(session: CloudSession, selected: CloudAck): ByteArray {
                    val raw = http.download(session, selected)
                    val whenExported = expected.copy(exportedAt = expected.exportedAt.plusSeconds(5))
                    http.upload(session, CloudPending(UUID.randomUUID().toString(), selected.revision, encode(whenExported)))
                    return raw
                }
            }
            val replacing = target.repo(replacement)
            expect(CloudIssue.RestoreChanged) { replacing.restore(replacing.selectRestore()) }
            target.assertEmptyAndUnreconciled()

            // SQLite fails on the last table after the vehicle, entries and readings were inserted.
            target.db.openHelper.writableDatabase.execSQL(
                "CREATE TRIGGER fail_m54_restore BEFORE INSERT ON reminders BEGIN SELECT RAISE(ABORT, 'fixture'); END",
            )
            expect(CloudIssue.Storage) { target.repo.restore(target.repo.selectRestore()) }
            target.assertEmptyAndUnreconciled()
            target.db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_m54_restore")

            val otherVehicle = vehicle.copy(id = id(42))
            val localRace = object : CloudApi by http {
                override suspend fun download(session: CloudSession, selected: CloudAck): ByteArray =
                    http.download(session, selected).also { target.db.vehicleDao().insert(otherVehicle) }
            }
            val racing = target.repo(localRace)
            expect(CloudIssue.PopulatedLedger) { racing.restore(racing.selectRestore()) }
            val afterRace = capture(target.backup)
            assertEquals(BackupSnapshot(afterRace.exportedAt, vehicles = listOf(otherVehicle)), afterRace)
            assertEquals(0, target.reconciled)
            assertTrue(target.delivery.wasDelivered(UUID.fromString(due.id)))
            target.store.write(target.store.read().copy(
                session = CloudSession(current.accountId, current.token, Instant.EPOCH, current.username),
            ))
            expect(CloudIssue.Expired) { target.repo.selectRestore() }
            assertNull(target.store.read().session)
        }
    }

    @Test fun recoveryCodeRedemptionRotatesTheCodeAndRevokesAnEarlierSession() = runBlocking<Unit> {
        fixture { target ->
            val name = "m54_recovery_" + UUID.randomUUID().toString().take(8)
            val code = checkNotNull(target.repo.signIn(name, password, checkNotNull(arguments.getString("invite"))))
            val old = checkNotNull(target.store.read().session)
            val new = password + " new"
            val replacement = checkNotNull(target.repo.recoverAccount(name, code, new))
            assertTrue(cloudCode.matches(replacement))
            assertNotEquals(code, replacement)
            expect(CloudIssue.Expired) { LocalCloudApi().metadata(old) }
            expect(CloudIssue.Credentials) { target.repo.recoverAccount(name, code, new) }
            assertEquals(name, target.repo.status().username)
            assertFalse(File(target.directory, "local-cloud-v1").readBytes().decodeToString().contains(replacement))
        }
    }

    private suspend fun fixture(work: suspend (Fixture) -> Unit) {
        val fixture = Fixture()
        try {
            work(fixture)
        } finally {
            fixture.db.close()
            fixture.directory.deleteRecursively()
        }
    }

    private class Fixture {
        val db = Room.inMemoryDatabaseBuilder(context, VehicleDatabase::class.java).addCallback(reminderConstraints).build()
        val directory = File(context.noBackupFilesDir, "m54-test-${UUID.randomUUID()}").apply { mkdirs() }
        val store = KeystoreCloudStore(object : ContextWrapper(context) {
            override fun getNoBackupFilesDir() = directory
        })
        val delivery = ReminderDeliveryStore(File(directory, "markers"))
        var reconciled = 0
        val backup = BackupRepository(db, ReminderNotifications(
            context, { emptyList() }, deliveryDirectory = File(directory, "markers"),
        ), { reconciled++ })
        val repo = repo(LocalCloudApi())

        init { delivery.markDelivered(UUID.fromString(due.id)) }

        fun repo(api: CloudApi) = CloudRepository(api, store, backup::captureSnapshot, backup::bindIfEmpty, backup = backup)

        suspend fun session(): CloudSession {
            repo.signIn(account, password, null)
            repo.bind()
            return checkNotNull(store.read().session)
        }

        suspend fun assertEmptyAndUnreconciled() {
            assertTrue(backup.isEmpty())
            assertTrue(delivery.wasDelivered(UUID.fromString(due.id)))
            assertEquals(0, reconciled)
            assertNull(store.read().restoredRevision)
        }
    }
}

@RunWith(AndroidJUnit4::class)
class CloudRecoverySettingsDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Before fun requireLocalFixture() = requireFixture()

    @Test fun confirmAcknowledgedRevisionThenShowRecoveredVehicleAndRejectSecondRestore() {
        val marker = UUID.fromString(due.id)
        val delivery = ReminderDeliveryStore(File(context.noBackupFilesDir, "reminder-delivery"))
        runBlocking {
            assertTrue(app.container.backupRepository.isEmpty())
            delivery.markDelivered(marker)
        }
        tap("homeOpenSettings")
        tap("settingsCloud")
        waitFor("cloudRestoreReview")
        tap("cloudRestoreReview")
        waitFor("cloudRestoreRevision")
        val ack = runBlocking {
            val session = checkNotNull(KeystoreCloudStore(context).read().session)
            checkNotNull(LocalCloudApi().metadata(session).snapshot)
        }
        compose.onNodeWithText(ack.revision).assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        waitFor("cloudRestoreRevision")
        compose.onNodeWithText(ack.revision).assertIsDisplayed()
        assertTrue(runBlocking { app.container.backupRepository.isEmpty() })
        compose.onNodeWithTag("cloudConfirmRestore").performClick()
        waitForText(R.string.cloud_restored)
        assertFalse(delivery.wasDelivered(marker))
        val restored = runBlocking { capture() }
        assertEquals(expected, restored.copy(exportedAt = expected.exportedAt))
        tap("cloudRestoreReview")
        waitForText(R.string.cloud_populated)
        compose.onNodeWithText(compose.activity.getString(R.string.ledger_back)).performScrollTo().performClick()
        compose.onNodeWithText(compose.activity.getString(R.string.ledger_back)).performScrollTo().performClick()
        compose.waitUntil(12_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText(vehicle.displayLabel)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun tap(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick() }
    private fun waitFor(tag: String) {
        compose.waitUntil(12_000) { compose.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun waitForText(resource: Int) {
        val label = compose.activity.getString(resource)
        compose.waitUntil(15_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasText(label)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
