package org.unishiasy.vehiclelog.data.backup

import java.io.ByteArrayOutputStream
import java.io.InputStream

// Read at most one byte beyond the limit, independent of provider metadata.
internal fun InputStream.readBackupBytes(): ByteArray? {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (output.size() < BackupCodec.MAX_BYTES) {
        val count = read(buffer, 0, minOf(buffer.size, BackupCodec.MAX_BYTES - output.size()))
        if (count < 0) return output.toByteArray()
        if (count == 0) {
            val byte = read()
            if (byte < 0) return output.toByteArray()
            output.write(byte)
        } else {
            output.write(buffer, 0, count)
        }
    }
    return if (read() == -1) output.toByteArray() else null
}
