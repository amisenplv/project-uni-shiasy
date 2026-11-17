package org.unishiasy.vehiclelog.data.cloud

import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.unishiasy.vehiclelog.data.backup.BackupResult
import org.unishiasy.vehiclelog.data.backup.BackupCodec
import org.unishiasy.vehiclelog.data.backup.BackupOutcome
import org.unishiasy.vehiclelog.data.backup.BackupRepository

internal enum class CloudBinding { Unbound, Bound, OtherAccount }
internal enum class CloudRetry { None, Pending, Review }

internal data class CloudStatus(
    val username: String? = null,
    val expires: Instant? = null,
    val binding: CloudBinding = CloudBinding.Unbound,
    val retry: CloudRetry = CloudRetry.None,
    val ack: CloudAck? = null,
    val expired: Boolean = false,
)

internal class CloudRestoreSelection(
    val snapshot: CloudAck,
    private val session: CloudSession,
    private val binding: String?,
) {
    fun matches(current: CloudSession, owner: String?) =
        current.accountId == session.accountId && current.token == session.token && owner == binding
}

internal class CloudRepository(
    private val api: CloudApi,
    private val store: CloudStore,
    private val capture: suspend () -> BackupResult<ByteArray>,
    private val bindIfEmpty: suspend (() -> Unit) -> Boolean,
    private val clock: Clock = Clock.systemUTC(),
    private val attemptId: () -> String = { UUID.randomUUID().toString() },
    private val backup: BackupRepository? = null,
) {
    private val mutex = Mutex()

    suspend fun status(): CloudStatus = operation {
        val record = stored()
        val session = record.session ?: return@operation CloudStatus()
        if (session.expires <= clock.instant()) {
            save(record.copy(session = null))
            return@operation CloudStatus(expired = true)
        }
        val binding = when (record.owner) {
            null -> CloudBinding.Unbound
            session.accountId -> CloudBinding.Bound
            else -> CloudBinding.OtherAccount
        }
        CloudStatus(session.username, session.expires, binding, when {
            binding != CloudBinding.Bound || record.pending == null -> CloudRetry.None
            record.pending.reviewRequired -> CloudRetry.Review
            else -> CloudRetry.Pending
        }, record.ack.takeIf { binding == CloudBinding.Bound })
    }

    suspend fun signIn(username: String, password: String, invite: String?): String? =
        authenticate(username, password, invite) { api.authenticate(username, password, invite) }

    suspend fun recoverAccount(username: String, recoveryCode: String, newPassword: String): String? =
        authenticate(username, newPassword, recoveryCode) { api.recover(username, recoveryCode, newPassword) }

    private suspend fun authenticate(
        username: String,
        password: String,
        code: String?,
        request: suspend () -> CloudSignIn,
    ): String? = operation {
        if (!Regex("[a-z0-9._-]{3,32}").matches(username) ||
            password.codePointCount(0, password.length) !in 15..128 ||
            !Charsets.UTF_8.newEncoder().canEncode(password) ||
            code != null && !cloudCode.matches(code)
        ) throw CloudFailure(CloudIssue.Input)
        val record = stored()
        val response = request()
        if (response.session.expires <= clock.instant()) throw CloudFailure(CloudIssue.Expired)
        save(record.copy(session = response.session))
        response.recoveryCode
    }

    suspend fun signOut() = operation {
        val record = stored()
        val session = record.session ?: return@operation
        save(record.copy(session = null))
        try {
            api.logout(session)
        } catch (_: CloudFailure) {
            // Local sign-out is already durable, including during offline use.
        }
    }

    suspend fun bind() = operation {
        val record = stored()
        val session = active(record)
        if (record.owner == session.accountId) return@operation
        val bound = CloudRecord(session = session, owner = session.accountId)
        if (record.owner == null) save(bound)
        else if (!bindIfEmpty { save(bound) }) throw CloudFailure(CloudIssue.PopulatedLedger)
    }

    suspend fun startUpload(): CloudSlot? = operation {
        val record = stored()
        val session = active(record, bound = true)
        if (record.pending != null) throw CloudFailure(CloudIssue.Conflict)
        val server = authorized(record) { api.metadata(session) }
        if (server.snapshot != null && server.revision != record.ack?.revision) return@operation server
        newUpload(record, server.revision)
        null
    }

    suspend fun review(): CloudSlot = operation {
        val record = stored()
        val session = active(record, bound = true)
        authorized(record) { api.metadata(session) }
    }

    suspend fun replaceAfterConfirmation(expected: CloudSlot) = operation {
        val record = stored()
        active(record, bound = true)
        newUpload(record, expected.revision)
    }

    suspend fun retry() = operation {
        val record = stored()
        val session = active(record, bound = true)
        val pending = record.pending ?: throw CloudFailure(CloudIssue.Conflict)
        if (pending.reviewRequired) throw CloudFailure(CloudIssue.Conflict)
        send(record, session, pending)
    }

    suspend fun selectRestore(): CloudRestoreSelection = operation {
        val record = stored()
        val session = active(record, bound = true)
        if (!checkNotNull(backup).isEmpty()) throw CloudFailure(CloudIssue.PopulatedLedger)
        val snapshot = authorized(record) { api.metadata(session) }.snapshot
            ?: throw CloudFailure(CloudIssue.NoSnapshot)
        CloudRestoreSelection(snapshot, session, record.owner)
    }

    suspend fun restore(selection: CloudRestoreSelection): CloudIssue = operation {
        val repository = checkNotNull(backup)
        val record = stored()
        val session = active(record, bound = true)
        val selected = selection.snapshot
        fun verifyLocal() {
            val latest = stored()
            if (!selection.matches(active(latest, bound = true), latest.owner)) {
                throw CloudFailure(CloudIssue.RestoreChanged)
            }
            if (selected.expires <= clock.instant()) throw CloudFailure(CloudIssue.NoSnapshot)
        }
        verifyLocal()
        if (!repository.isEmpty()) throw CloudFailure(CloudIssue.PopulatedLedger)
        val bytes = authorized(record) { api.download(session, selected) }
        try {
            if (bytes.size > BackupCodec.MAX_BYTES) throw CloudFailure(CloudIssue.DownloadTooLarge)
            if (bytes.size != selected.length) throw CloudFailure(CloudIssue.DownloadInterrupted)
            if (bytes.sha256() != selected.digest) throw CloudFailure(CloudIssue.RestoreInvalid)
            val outcome = repository.restoreBytes(bytes, verifyRemote = {
                val current = authorized(record) { api.metadata(session) }.snapshot
                    ?: throw CloudFailure(CloudIssue.NoSnapshot)
                if (current != selected) throw CloudFailure(CloudIssue.RestoreChanged)
            }, verifyLocal = ::verifyLocal)
            when (outcome) {
                BackupOutcome.Restored, BackupOutcome.RestoredWithNotificationWarning -> {
                    val saved = withContext(NonCancellable) {
                        try {
                            save(stored().copy(restoredRevision = selected.revision))
                            true
                        } catch (_: CloudFailure) {
                            false
                        }
                    }
                    if (saved && outcome == BackupOutcome.Restored) CloudIssue.Restored
                    else CloudIssue.RestoredWithWarning
                }
                BackupOutcome.LedgerPopulated -> throw CloudFailure(CloudIssue.PopulatedLedger)
                BackupOutcome.TooLarge -> throw CloudFailure(CloudIssue.DownloadTooLarge)
                BackupOutcome.UnsupportedFormat, BackupOutcome.InvalidData -> throw CloudFailure(CloudIssue.RestoreInvalid)
                else -> throw CloudFailure(CloudIssue.Storage)
            }
        } finally {
            bytes.fill(0)
        }
    }

    private suspend fun newUpload(record: CloudRecord, base: String) {
        val bytes = when (val result = capture()) {
            is BackupResult.Success -> result.value
            is BackupResult.Failure -> throw CloudFailure(CloudIssue.InvalidSnapshot)
        }
        val pending = CloudPending(attemptId(), base, bytes)
        currentCoroutineContext().ensureActive()
        val staged = record.copy(pending = pending)
        save(staged)
        currentCoroutineContext().ensureActive()
        send(staged, active(staged, bound = true), pending)
    }

    private suspend fun send(record: CloudRecord, session: CloudSession, pending: CloudPending) {
        val staged = record.copy(pending = pending)
        try {
            val ack = authorized(staged) { api.upload(session, pending) }
            if (ack.attempt != pending.attempt || ack.revision == pending.base ||
                ack.length != pending.bytes.size || ack.digest != pending.bytes.sha256()
            ) throw CloudFailure(CloudIssue.Unconfirmed)
            save(staged.copy(ack = ack, pending = null))
        } catch (error: CloudFailure) {
            if (error.issue == CloudIssue.Conflict || error.issue == CloudIssue.Rejected) {
                save(staged.copy(pending = pending.requireReview()))
            }
            throw error
        }
    }

    private fun active(record: CloudRecord, bound: Boolean = false): CloudSession {
        val session = record.session ?: throw CloudFailure(CloudIssue.SignedOut)
        if (session.expires <= clock.instant()) {
            save(record.copy(session = null))
            throw CloudFailure(CloudIssue.Expired)
        }
        if (bound && record.owner != session.accountId) {
            throw CloudFailure(if (record.owner == null) CloudIssue.BindingRequired else CloudIssue.DifferentAccount)
        }
        return session
    }

    private suspend fun <T> authorized(record: CloudRecord, block: suspend () -> T): T = try {
        block()
    } catch (error: CloudFailure) {
        if (error.issue == CloudIssue.Expired) save(record.copy(session = null))
        throw error
    }

    private suspend fun <T> operation(block: suspend () -> T): T =
        withContext(Dispatchers.IO) { mutex.withLock { block() } }

    private fun stored(): CloudRecord = storage(store::read)
    private fun save(record: CloudRecord) = storage { store.write(record) }
    private fun <T> storage(block: () -> T): T = try {
        block()
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) {
        throw CloudFailure(CloudIssue.Storage)
    }
}

internal fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
    .joinToString("") { "%02x".format(it) }
