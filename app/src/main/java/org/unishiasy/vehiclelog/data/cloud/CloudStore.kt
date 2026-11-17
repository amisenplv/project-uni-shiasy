package org.unishiasy.vehiclelog.data.cloud

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import java.time.Instant
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.unishiasy.vehiclelog.data.backup.BackupCodec

internal class CloudPending(
    val attempt: String,
    val base: String,
    val bytes: ByteArray,
    val reviewRequired: Boolean = false,
) {
    fun requireReview() = CloudPending(attempt, base, bytes, true)
}

internal data class CloudRecord(
    val session: CloudSession? = null,
    val owner: String? = null,
    val ack: CloudAck? = null,
    val pending: CloudPending? = null,
    val restoredRevision: String? = null,
)

internal interface CloudStore {
    fun read(): CloudRecord
    fun write(record: CloudRecord)
}

internal class KeystoreCloudStore(context: Context) : CloudStore {
    private val file = AtomicFile(File(context.noBackupFilesDir, "local-cloud-v1"))
    private val alias = "vehiclelog-local-cloud-v1"
    private val maximum = BackupCodec.MAX_BYTES + 4096

    override fun read(): CloudRecord {
        val encrypted = try {
            readEnvelope()
        } catch (_: FileNotFoundException) {
            // Fail closed if an existing file cannot be opened.
            if (file.baseFile.exists()) throw CloudFailure(CloudIssue.Storage)
            return CloudRecord()
        }
        check(encrypted.size >= 29 && encrypted[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, encrypted, 1, 12))
        return DataInputStream(ByteArrayInputStream(cipher.doFinal(encrypted, 13, encrypted.size - 13))).use { input ->
            val session = if (input.readBoolean()) {
                CloudSession(input.readUTF(), input.readUTF(), input.instant(), input.readUTF())
            } else null
            val owner = if (input.readBoolean()) input.readUTF() else null
            val ack = if (input.readBoolean()) {
                CloudAck(input.readUTF(), input.readUTF(), input.instant(), input.instant(), input.readInt(), input.readUTF())
            } else null
            val pending = if (input.readBoolean()) {
                val attempt = input.readUTF()
                val base = input.readUTF()
                val review = input.readBoolean()
                val size = input.readInt().also { check(it in 1..BackupCodec.MAX_BYTES) }
                CloudPending(attempt, base, ByteArray(size).also(input::readFully), review)
            } else null
            val restoredRevision = when (input.read()) {
                -1 -> null // M5.3 records end here.
                1 -> input.readUTF().also { check(cloudUuid.matches(it)) }
                else -> error("Invalid cloud state")
            }
            check(input.read() == -1)
            check(owner != null || ack == null && pending == null && restoredRevision == null)
            CloudRecord(session, owner, ack, pending, restoredRevision)
        }
    }

    override fun write(record: CloudRecord) {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { out ->
            out.writeBoolean(record.session != null)
            record.session?.let {
                out.writeUTF(it.accountId)
                out.writeUTF(it.token)
                out.instant(it.expires)
                out.writeUTF(it.username)
            }
            out.writeBoolean(record.owner != null)
            record.owner?.let(out::writeUTF)
            out.writeBoolean(record.ack != null)
            record.ack?.let {
                out.writeUTF(it.revision)
                out.writeUTF(it.attempt)
                out.instant(it.time)
                out.instant(it.expires)
                out.writeInt(it.length)
                out.writeUTF(it.digest)
            }
            out.writeBoolean(record.pending != null)
            record.pending?.let {
                out.writeUTF(it.attempt)
                out.writeUTF(it.base)
                out.writeBoolean(it.reviewRequired)
                out.writeInt(it.bytes.size)
                out.write(it.bytes)
            }
            record.restoredRevision?.let {
                out.writeByte(1)
                out.writeUTF(it)
            }
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(create = !file.baseFile.exists()))
        val iv = cipher.iv.also { check(it.size == 12) }
        val encrypted = byteArrayOf(1) + iv + cipher.doFinal(bytes.toByteArray())
        check(encrypted.size <= maximum)
        val output = file.startWrite()
        try {
            output.write(encrypted)
            output.fd.sync()
            file.finishWrite(output)
        } catch (failure: Exception) {
            file.failWrite(output)
            throw failure
        }
        // AtomicFile reports some finish failures through its own logger. Verify
        // the committed bytes before the caller is allowed to transmit anything.
        check(readEnvelope().contentEquals(encrypted))
    }

    private fun readEnvelope() = file.openRead().use { raw ->
        val size = raw.channel.size().also { check(it in 1..maximum.toLong()) }
        val input = DataInputStream(raw)
        ByteArray(size.toInt()).also {
            input.readFully(it)
            check(input.read() == -1)
        }
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }

    private fun DataOutputStream.instant(value: Instant) = writeUTF(value.toString())
    private fun DataInputStream.instant(): Instant = Instant.parse(readUTF())
}
