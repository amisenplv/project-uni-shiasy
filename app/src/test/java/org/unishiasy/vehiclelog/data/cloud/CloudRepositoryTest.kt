package org.unishiasy.vehiclelog.data.cloud

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.unishiasy.vehiclelog.data.backup.BackupResult

class CloudRepositoryTest {
    @Test fun signedOutAndStartupHaveNoNetworkActivity() = runBlocking {
        val fixture = Fixture()
        assertNull(fixture.repo.status().username)
        expect(CloudIssue.SignedOut) { fixture.repo.startUpload() }
        assertEquals(0, fixture.api.requests)
        fixture.signIn()
        val count = fixture.api.requests
        repeat(3) { fixture.newRepo().status() }
        assertEquals(count, fixture.api.requests)
    }

    @Test fun registrationRecoveryCodeIsReturnedWithoutBeingPersistedAndSignOutKeepsBinding() = runBlocking {
        val fixture = Fixture()
        val code = fixture.repo.signIn("first", Fixture.password, "a".repeat(32))
        assertEquals("b".repeat(32), code)
        fixture.repo.bind()
        fixture.api.failLogout = true
        fixture.repo.signOut()
        assertNull(fixture.store.record.session)
        assertEquals("first", fixture.store.record.owner)
        assertNull(fixture.newRepo().status().username)
    }

    @Test fun revokedAndExpiredSessionsPreservePendingAndLedgerBinding() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        fixture.api.uploadIssue = CloudIssue.Expired
        expect(CloudIssue.Expired) { fixture.repo.startUpload() }
        val pending = checkNotNull(fixture.store.record.pending)
        assertNull(fixture.store.record.session)
        assertEquals("first", fixture.store.record.owner)
        fixture.signIn()
        fixture.clock.current = fixture.api.session.expires
        assertTrue(fixture.newRepo().status().expired)
        assertNull(fixture.store.record.session)
        assertEquals(pending, fixture.store.record.pending)
        val count = fixture.api.requests
        expect(CloudIssue.SignedOut) { fixture.newRepo().retry() }
        assertEquals(count, fixture.api.requests)
    }

    @Test fun attemptIsDurableBeforeSendAndAckPersistsOnlyWithExactReceipt() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        fixture.api.onUpload = { sending ->
            assertEquals(sending, fixture.store.record.pending)
            assertNull(fixture.store.record.ack)
        }
        fixture.api.badReceipt = true
        expect(CloudIssue.Unconfirmed) { fixture.repo.startUpload() }
        assertNull(fixture.store.record.ack)
        fixture.api.badReceipt = false
        fixture.repo.retry()
        assertEquals("new-revision", fixture.newRepo().status().ack?.revision)
        assertNull(fixture.store.record.pending)
    }

    @Test fun failedStagingStopsNetworkUpload() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        fixture.store.failWrite = true
        expect(CloudIssue.Storage) { fixture.repo.startUpload() }
        assertTrue(fixture.api.uploads.isEmpty())
    }

    @Test fun lostAcknowledgementAndNetworkLossKeepExactBytesAcrossRestartAndNewLocalEdits() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        val original = fixture.bytes.copyOf()
        fixture.api.uploadIssue = CloudIssue.Unconfirmed
        expect(CloudIssue.Unconfirmed) { fixture.repo.startUpload() }
        fixture.bytes = "later ledger".encodeToByteArray()
        fixture.api.uploadIssue = null
        val restarted = fixture.newRepo()
        assertEquals(CloudRetry.Pending, restarted.status().retry)
        assertEquals(1, fixture.api.uploads.size)
        restarted.retry()
        assertEquals(1, fixture.captures)
        val pair = fixture.api.uploads
        assertEquals(pair[0].attempt, pair[1].attempt)
        assertEquals(pair[0].base, pair[1].base)
        assertArrayEquals(original, pair[1].bytes)
        assertNull(fixture.store.record.pending)
    }

    @Test fun networkLossBeforePreparationLeavesNoAttemptOrConfirmedAck() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        fixture.api.metadataIssue = CloudIssue.Offline
        expect(CloudIssue.Offline) { fixture.repo.startUpload() }
        assertNull(fixture.store.record.pending)
        assertNull(fixture.store.record.ack)
        assertTrue(fixture.api.uploads.isEmpty())
    }

    @Test fun cancellationLeavesRetryAndNewInstanceDoesNotAutoSend() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        val started = CompletableDeferred<Unit>()
        val delay = CompletableDeferred<Unit>()
        fixture.api.onUpload = {
            started.complete(Unit)
            delay.await()
        }
        val job = launch {
            try { fixture.repo.startUpload() } catch (error: CancellationException) { throw error }
        }
        started.await()
        job.cancelAndJoin()
        assertTrue(job.isCancelled)
        val restarted = fixture.newRepo()
        assertEquals(CloudRetry.Pending, restarted.status().retry)
        assertEquals(1, fixture.api.uploads.size)
        fixture.api.onUpload = {}
        restarted.retry()
        assertEquals(fixture.api.uploads[0].attempt, fixture.api.uploads[1].attempt)
    }

    @Test fun conflictRequiresReviewedRevisionAndFreshExplicitConfirmation() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        fixture.api.uploadIssue = CloudIssue.Conflict
        expect(CloudIssue.Conflict) { fixture.repo.startUpload() }
        expect(CloudIssue.Conflict) { fixture.newRepo().retry() }
        val first = fixture.api.uploads.single()
        fixture.api.server = CloudSlot("new-base", fixture.api.ack("someone-else", fixture.bytes))
        val reviewed = fixture.repo.review()
        assertEquals(1, fixture.api.uploads.size)
        fixture.api.uploadIssue = null
        fixture.repo.replaceAfterConfirmation(reviewed)
        assertEquals("new-base", fixture.api.uploads.last().base)
        assertFalse(first.attempt == fixture.api.uploads.last().attempt)
    }

    @Test fun populatedLedgerBindingBlocksAnotherAccountWithoutNetworkOrDiscard() = runBlocking {
        val fixture = Fixture()
        fixture.signIn()
        fixture.api.uploadIssue = CloudIssue.Unconfirmed
        expect(CloudIssue.Unconfirmed) { fixture.repo.startUpload() }
        fixture.repo.signOut()
        fixture.repo.signIn("second", Fixture.password, null)
        val status = fixture.repo.status()
        assertEquals(CloudBinding.OtherAccount, status.binding)
        assertEquals(CloudRetry.None, status.retry)
        assertNull(status.ack)
        val calls = fixture.api.requests
        expect(CloudIssue.DifferentAccount) { fixture.repo.retry() }
        expect(CloudIssue.PopulatedLedger) { fixture.repo.bind() }
        assertEquals(calls, fixture.api.requests)
        assertEquals("first", fixture.store.record.owner)
        assertTrue(fixture.store.record.pending != null)
        fixture.empty = true
        fixture.repo.bind()
        assertEquals("second", fixture.store.record.owner)
        assertNull(fixture.store.record.pending)
    }

    private suspend fun expect(issue: CloudIssue, operation: suspend () -> Unit) {
        assertEquals(issue, runCatching { operation() }.exceptionOrNull()?.let { (it as? CloudFailure)?.issue })
    }

    private class Fixture {
        companion object { const val password = "synthetic test password" }
        val clock = MutableClock()
        val api = FakeApi(clock)
        val store = MemoryStore()
        var bytes = "snapshot encoded bytes".encodeToByteArray()
        var captures = 0
        var empty = false
        private var sequence = 0
        val repo = newRepo()
        fun newRepo() = CloudRepository(api, store, {
            captures++
            BackupResult.Success(bytes.copyOf())
        }, { if (empty) it(); empty }, clock, { "attempt-${++sequence}" })

        suspend fun signIn() {
            repo.signIn("first", password, null)
            repo.bind()
        }
    }

    private class MemoryStore : CloudStore {
        var record = CloudRecord()
        var failWrite = false
        override fun read() = record
        override fun write(record: CloudRecord) {
            check(!failWrite)
            this.record = record
        }
    }

    private class MutableClock : Clock() {
        var current: Instant = Instant.parse("2026-11-15T12:00:00Z")
        override fun instant() = current
        override fun withZone(zone: ZoneId) = this
        override fun getZone(): ZoneId = ZoneOffset.UTC
    }

    private class FakeApi(private val clock: Clock) : CloudApi {
        lateinit var session: CloudSession
        var requests = 0
        var server = CloudSlot("base-revision", null)
        val uploads = mutableListOf<CloudPending>()
        var uploadIssue: CloudIssue? = null
        var metadataIssue: CloudIssue? = null
        var failLogout = false
        var badReceipt = false
        var onUpload: suspend (CloudPending) -> Unit = {}

        override suspend fun authenticate(username: String, password: String, invite: String?): CloudSignIn {
            requests++
            session = CloudSession(username, "token", clock.instant().plusSeconds(3_600), username)
            return CloudSignIn(session, "b".repeat(32).takeIf { invite != null })
        }

        override suspend fun recover(username: String, recoveryCode: String, newPassword: String) =
            authenticate(username, newPassword, recoveryCode)

        override suspend fun download(session: CloudSession, selected: CloudAck): ByteArray =
            error("M5.3 tests do not download")

        override suspend fun logout(session: CloudSession) {
            requests++
            if (failLogout) throw CloudFailure(CloudIssue.Offline)
        }

        override suspend fun metadata(session: CloudSession): CloudSlot {
            requests++
            metadataIssue?.let { throw CloudFailure(it) }
            return server
        }

        override suspend fun upload(session: CloudSession, pending: CloudPending): CloudAck {
            requests++
            uploads += pending
            onUpload(pending)
            uploadIssue?.let { throw CloudFailure(it) }
            return ack(pending.attempt, pending.bytes).let { if (badReceipt) it.copy(digest = "bad") else it }
        }

        fun ack(attempt: String, bytes: ByteArray) = CloudAck("new-revision", attempt, clock.instant(),
            clock.instant().plusSeconds(3_600), bytes.size, bytes.sha256())
    }
}
