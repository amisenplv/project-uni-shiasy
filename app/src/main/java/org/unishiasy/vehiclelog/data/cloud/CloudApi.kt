package org.unishiasy.vehiclelog.data.cloud

import java.io.IOException
import java.net.HttpURLConnection
import java.net.Proxy
import java.net.URL
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.unishiasy.vehiclelog.data.backup.BackupCodec

internal val cloudUuid = Regex("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
internal val cloudToken = Regex("[0-9a-f]{64}")
internal val cloudCode = Regex("[0-9a-f]{32}")

internal class CloudSession(val accountId: String, val token: String, val expires: Instant, val username: String)
internal class CloudSignIn(val session: CloudSession, val recoveryCode: String?)
internal data class CloudAck(
    val revision: String,
    val attempt: String,
    val time: Instant,
    val expires: Instant,
    val length: Int,
    val digest: String,
)
internal data class CloudSlot(val revision: String, val snapshot: CloudAck?)

internal enum class CloudIssue {
    Input, Credentials, RegistrationConflict, SignedOut, Expired, BindingRequired, DifferentAccount,
    PopulatedLedger, RateLimited, Offline, Unconfirmed, Conflict, Rejected, InvalidSnapshot, Storage, Uploaded,
    NoSnapshot, RestoreChanged, DownloadInterrupted, DownloadTooLarge, RestoreInvalid,
    Restored, RestoredWithWarning,
}
internal class CloudFailure(val issue: CloudIssue) : Exception()

internal interface CloudApi {
    suspend fun authenticate(username: String, password: String, invite: String?): CloudSignIn
    suspend fun recover(username: String, recoveryCode: String, newPassword: String): CloudSignIn
    suspend fun download(session: CloudSession, selected: CloudAck): ByteArray
    suspend fun logout(session: CloudSession)
    suspend fun metadata(session: CloudSession): CloudSlot
    suspend fun upload(session: CloudSession, pending: CloudPending): CloudAck
}

internal class LocalCloudApi : CloudApi {
    override suspend fun authenticate(username: String, password: String, invite: String?): CloudSignIn {
        val request = buildJsonObject {
            put("username", username)
            put("password", password)
            if (invite != null) put("inviteCode", invite)
        }.toString().encodeToByteArray()
        val json = exchange("POST", if (invite == null) "auth/login" else "auth/register", null, request,
            expectedStatus = if (invite == null) 200 else 201).json()
        return json.signedIn(username, issuedCode = invite != null)
    }

    override suspend fun recover(username: String, recoveryCode: String, newPassword: String): CloudSignIn {
        val request = buildJsonObject {
            put("username", username)
            put("recoveryCode", recoveryCode)
            put("newPassword", newPassword)
        }.toString().encodeToByteArray()
        return exchange("POST", "auth/recover", null, request).json().signedIn(username, issuedCode = true)
    }

    override suspend fun download(session: CloudSession, selected: CloudAck): ByteArray =
        exchange("GET", "backup", session, selected = selected).body

    private fun JsonObject.signedIn(username: String, issuedCode: Boolean): CloudSignIn {
        val account = uuid("accountId")
        val token = string("sessionToken").takeIf(cloudToken::matches) ?: invalidReply()
        val expires = instant("sessionExpiresAt")
        val recovery = if (issuedCode) {
            instant("recoveryExpiresAt")
            string("recoveryCode").takeIf(cloudCode::matches) ?: invalidReply()
        } else null
        return CloudSignIn(CloudSession(account, token, expires, username), recovery)
    }

    override suspend fun logout(session: CloudSession) {
        exchange("POST", "auth/logout", session, expectedStatus = 204)
    }

    override suspend fun metadata(session: CloudSession): CloudSlot =
        exchange("GET", "backup/metadata", session).slot()

    override suspend fun upload(session: CloudSession, pending: CloudPending): CloudAck =
        exchange("PUT", "backup", session, pending.bytes, pending).slot().snapshot ?: invalidReply()

    private suspend fun exchange(
        method: String,
        path: String,
        session: CloudSession?,
        bytes: ByteArray? = null,
        pending: CloudPending? = null,
        expectedStatus: Int = 200,
        selected: CloudAck? = null,
    ): Reply = runInterruptible(Dispatchers.IO) {
        // The client only supplies fixed route suffixes. No proxy, redirect, or external URL is accepted.
        val connection = URL("http://127.0.0.1:8765/v1/$path").openConnection(Proxy.NO_PROXY) as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.useCaches = false
            connection.connectTimeout = 5_000
            connection.readTimeout = 10_000
            connection.requestMethod = method
            session?.let { connection.setRequestProperty("Authorization", "Bearer ${it.token}") }
            if (pending != null) {
                connection.setRequestProperty("If-Match", "\"${pending.base}\"")
                connection.setRequestProperty("Idempotency-Key", pending.attempt)
                connection.setRequestProperty("Snapshot-Format", "snapshot-v1")
            }
            if (selected != null) {
                connection.setRequestProperty("If-Match", "\"${selected.revision}\"")
                connection.setRequestProperty("Accept-Encoding", "identity")
            }
            if (bytes != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }
            val status = connection.responseCode
            if (selected != null && status == expectedStatus) {
                return@runInterruptible Reply(connection.readSnapshot(selected), connection.getHeaderField("ETag"))
            }
            val body = (if (status == expectedStatus) connection.inputStream else connection.errorStream)
                ?.use { it.readCloudReply() } ?: byteArrayOf()
            if (body.size > 16_384) invalidReply()
            val reply = Reply(body, connection.getHeaderField("ETag"))
            if (status != expectedStatus) {
                val code = runCatching { (reply.json()["error"] as? JsonObject)?.string("code") }.getOrNull()
                throw CloudFailure(when {
                    code == "invalid_session" && status == 401 -> CloudIssue.Expired
                    code == "invalid_credentials" && status == 401 -> CloudIssue.Credentials
                    code == "registration_conflict" && status == 409 -> CloudIssue.RegistrationConflict
                    selected != null && status == 404 && code == "snapshot_missing" -> CloudIssue.NoSnapshot
                    selected != null && status == 412 && code == "revision_conflict" -> CloudIssue.RestoreChanged
                    status == 429 -> CloudIssue.RateLimited
                    status == 412 && code == "revision_conflict" ||
                        status == 409 && code in setOf("attempt_superseded", "attempt_mismatch") -> CloudIssue.Conflict
                    status == 413 && code == "body_too_large" ||
                        status == 422 && code in setOf("snapshot_invalid", "snapshot_incompatible") -> CloudIssue.Rejected
                    else -> if (pending != null) CloudIssue.Unconfirmed else CloudIssue.Offline
                })
            }
            reply
        } catch (_: IOException) {
            throw CloudFailure(when {
                selected != null -> CloudIssue.DownloadInterrupted
                pending != null -> CloudIssue.Unconfirmed
                else -> CloudIssue.Offline
            })
        } finally {
            connection.disconnect()
        }
    }

    private class Reply(val body: ByteArray, private val etag: String?) {
        fun json(): JsonObject = try {
            Json.parseToJsonElement(body.decodeToString(throwOnInvalidSequence = true)) as? JsonObject ?: invalidReply()
        } catch (_: IllegalArgumentException) {
            invalidReply()
        } catch (_: java.nio.charset.CharacterCodingException) {
            invalidReply()
        }

        fun slot(): CloudSlot {
            val json = json()
            val revision = json.uuid("slotRevision")
            if (etag != "\"$revision\"") invalidReply()
            val snapshot = json["snapshot"]
            if (snapshot === JsonNull) return CloudSlot(revision, null)
            val data = snapshot as? JsonObject ?: invalidReply()
            if (data.string("payloadFormat") != "snapshot-v1") invalidReply()
            val length = (data["byteLength"] as? JsonPrimitive)?.takeUnless { it.isString }
                ?.content?.toIntOrNull()?.takeIf { it in 1..BackupCodec.MAX_BYTES } ?: invalidReply()
            val at = data.instant("acknowledgedAt")
            val expires = data.instant("expiresAt").takeIf { it > at } ?: invalidReply()
            val sha256 = data.string("sha256").takeIf(cloudToken::matches) ?: invalidReply()
            return CloudSlot(revision, CloudAck(revision, data.uuid("attemptId"), at, expires, length, sha256))
        }
    }
}

private fun JsonObject.string(key: String): String =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: invalidReply()

private fun JsonObject.uuid(key: String): String = string(key).takeIf(cloudUuid::matches) ?: invalidReply()

private fun JsonObject.instant(key: String): Instant =
    runCatching { Instant.parse(string(key)) }.getOrElse { invalidReply() }

private fun invalidReply(): Nothing = throw CloudFailure(CloudIssue.Unconfirmed)

private fun java.io.InputStream.readCloudReply(): ByteArray {
    val buffer = ByteArray(16_385)
    var offset = 0
    while (offset < buffer.size) {
        val count = read(buffer, offset, buffer.size - offset)
        if (count < 0) break
        offset += count
    }
    return buffer.copyOf(offset)
}

// A conforming download has a known length. Allocate only the selected, bounded size.
private fun HttpURLConnection.readSnapshot(selected: CloudAck): ByteArray {
    val length = getHeaderField("Content-Length")?.toLongOrNull()
    if (length != null && length > BackupCodec.MAX_BYTES) throw CloudFailure(CloudIssue.DownloadTooLarge)
    if (getHeaderField("ETag") != "\"${selected.revision}\"") throw CloudFailure(CloudIssue.RestoreChanged)
    val encoding = getHeaderField("Content-Encoding")
    if (getHeaderField("Snapshot-Format") != "snapshot-v1" ||
        !getHeaderField("Content-Type").orEmpty().substringBefore(';').trim().equals("application/json", true) ||
        encoding != null && !encoding.equals("identity", true)
    ) throw CloudFailure(CloudIssue.RestoreInvalid)
    if (length != selected.length.toLong() || selected.length !in 1..BackupCodec.MAX_BYTES) {
        throw CloudFailure(CloudIssue.DownloadInterrupted)
    }
    val bytes = ByteArray(selected.length)
    val deadline = System.nanoTime() + 30_000_000_000L
    try {
        inputStream.use { stream ->
            var offset = 0
            while (offset < bytes.size) {
                if (System.nanoTime() - deadline >= 0 || Thread.currentThread().isInterrupted) {
                    throw CloudFailure(CloudIssue.DownloadInterrupted)
                }
                val count = stream.read(bytes, offset, minOf(8192, bytes.size - offset))
                if (count <= 0) throw CloudFailure(CloudIssue.DownloadInterrupted)
                offset += count
            }
            if (stream.read() != -1) throw CloudFailure(CloudIssue.DownloadTooLarge)
        }
        return bytes
    } catch (failure: Exception) {
        bytes.fill(0)
        throw failure
    }
}
