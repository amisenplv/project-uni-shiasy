package org.unishiasy.vehiclelog.reminders

import android.util.AtomicFile
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID

internal class ReminderDeliveryStore(private val directory: File) {
    fun wasDelivered(id: UUID): Boolean {
        val file = marker(id)
        return try {
            file.openRead().use { it.read() == 1 }
        } catch (failure: FileNotFoundException) {
            if (file.baseFile.exists()) throw failure
            false
        }
    }

    fun markDelivered(id: UUID) {
        if (!directory.isDirectory && !directory.mkdirs() && !directory.isDirectory) {
            throw IOException("Could not create reminder delivery directory")
        }
        val file = marker(id)
        val stream = file.startWrite()
        try {
            stream.write(1)
            file.finishWrite(stream)
        } catch (failure: IOException) {
            file.failWrite(stream)
            throw failure
        }
        if (!wasDelivered(id)) throw IOException("Could not persist reminder delivery marker")
    }

    fun clear(id: UUID) {
        val file = File(directory, id.toString())
        if (file.exists() && !file.delete()) {
            throw IOException("Could not clear reminder delivery marker")
        }
        val backup = File(directory, "$id.bak")
        if (backup.exists()) backup.delete()
    }

    fun reset() {
        if (!directory.deleteRecursively() && directory.exists()) {
            throw IOException("Could not reset reminder delivery markers")
        }
    }

    private fun marker(id: UUID) = AtomicFile(File(directory, id.toString()))
}
