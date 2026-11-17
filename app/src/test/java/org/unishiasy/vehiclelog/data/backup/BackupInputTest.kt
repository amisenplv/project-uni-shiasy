package org.unishiasy.vehiclelog.data.backup

import java.io.ByteArrayInputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BackupInputTest {
    @Test fun acceptsExactlyTheByteLimit() {
        val bytes = ByteArray(BackupCodec.MAX_BYTES) { 0x41 }
        assertArrayEquals(bytes, bytes.inputStream().readBackupBytes())
    }

    @Test fun stopsAfterTheFirstExcessByteEvenWithAnUnknownLength() {
        val stream = object : ByteArrayInputStream(ByteArray(BackupCodec.MAX_BYTES + 8)) {
            override fun available(): Int = 0
            fun bytesRead() = pos
        }
        assertNull(stream.readBackupBytes())
        assertEquals(BackupCodec.MAX_BYTES + 1, stream.bytesRead())
    }

    @Test fun toleratesZeroAndShortBulkReadsWithoutLosingBytes() {
        val expected = "دفتر خودرو".encodeToByteArray()
        val stream = object : ByteArrayInputStream(expected) {
            var first = true
            override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
                if (first) {
                    first = false
                    return 0
                }
                return super.read(bytes, offset, minOf(length, 2))
            }
        }
        assertArrayEquals(expected, stream.readBackupBytes())
    }
}
