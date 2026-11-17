package org.unishiasy.vehiclelog.data.backup

import android.database.sqlite.SQLiteException
import androidx.room.withTransaction
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Clock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.reminders.ReminderNotifications
import org.unishiasy.vehiclelog.reminders.RestoreNotificationResult

internal enum class BackupOutcome {
    Exported, Restored, RestoredWithNotificationWarning, Canceled, LedgerPopulated,
    TooLarge, UnsupportedFormat, InvalidData, FileUnavailable, StorageFailure,
}

internal class BackupRepository(
    private val database: VehicleDatabase,
    private val notifications: ReminderNotifications,
    private val reconcileSchedule: () -> Unit,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val dao = database.backupDao()

    suspend fun export(open: (() -> OutputStream?)?): BackupOutcome {
        if (open == null) return BackupOutcome.Canceled
        return backupOperation {
            val bytes = when (val result = captureSnapshot()) {
                is BackupResult.Success -> result.value
                is BackupResult.Failure -> return@backupOperation result.reason.toOutcome()
            }
            val stream = open() ?: return@backupOperation BackupOutcome.FileUnavailable
            stream.use { it.write(bytes) }
            BackupOutcome.Exported
        }
    }

    internal suspend fun captureSnapshot(): BackupResult<ByteArray> = withContext(Dispatchers.IO) {
        val snapshot = database.withTransaction {
            BackupSnapshot(
                clock.instant(),
                dao.vehicles(BackupCodec.MAX_VEHICLES + 1),
                dao.entries(BackupCodec.MAX_ENTRIES + 1),
                dao.readings(BackupCodec.MAX_READINGS + 1),
                dao.reminders(BackupCodec.MAX_REMINDERS + 1),
            )
        }
        BackupCodec.encode(snapshot)
    }

    internal suspend fun bindIfEmpty(bind: () -> Unit): Boolean = database.withTransaction {
        if (!dao.isEmpty()) return@withTransaction false
        bind()
        true
    }

    suspend fun restore(open: (() -> InputStream?)?): BackupOutcome {
        if (open == null) return BackupOutcome.Canceled
        return backupOperation {
            if (!dao.isEmpty()) return@backupOperation BackupOutcome.LedgerPopulated
            val stream = open() ?: return@backupOperation BackupOutcome.FileUnavailable
            val bytes = stream.use { it.readBackupBytes() }
                ?: return@backupOperation BackupOutcome.TooLarge
            restoreBytes(bytes)
        }
    }

    internal suspend fun isEmpty(): Boolean = withContext(Dispatchers.IO) { dao.isEmpty() }

    internal suspend fun restoreBytes(
        bytes: ByteArray,
        verifyRemote: suspend () -> Unit = {},
        verifyLocal: () -> Unit = {},
    ): BackupOutcome = backupOperation {
        val snapshot = when (val result = BackupCodec.decode(bytes)) {
            is BackupResult.Success -> result.value
            is BackupResult.Failure -> return@backupOperation result.reason.toOutcome()
        }
        verifyRemote()
        currentCoroutineContext().ensureActive()
        when (notifications.restoreAndReset(reconcileSchedule) {
            database.withTransaction {
                verifyLocal()
                if (!dao.isEmpty()) return@withTransaction false
                snapshot.vehicles.forEach { database.vehicleDao().insert(it) }
                snapshot.entries.forEach { database.ledgerDao().insert(it) }
                snapshot.readings.forEach { database.odometerDao().insert(it) }
                snapshot.reminders.forEach { database.reminderDao().insert(it) }
                true
            }
        }) {
            RestoreNotificationResult.LedgerPopulated -> BackupOutcome.LedgerPopulated
            RestoreNotificationResult.CleanupFailed -> BackupOutcome.RestoredWithNotificationWarning
            RestoreNotificationResult.Reset -> BackupOutcome.Restored
        }
    }

    private suspend fun backupOperation(operation: suspend () -> BackupOutcome): BackupOutcome =
        withContext(Dispatchers.IO) {
            try {
                operation()
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: SQLiteException) {
                BackupOutcome.StorageFailure
            } catch (_: IllegalStateException) {
                BackupOutcome.StorageFailure
            } catch (_: IOException) {
                BackupOutcome.FileUnavailable
            } catch (_: SecurityException) {
                BackupOutcome.FileUnavailable
            } catch (_: IllegalArgumentException) {
                BackupOutcome.FileUnavailable
            }
        }
}

private fun BackupError.toOutcome(): BackupOutcome = when (this) {
    BackupError.SizeLimit, BackupError.CollectionLimit -> BackupOutcome.TooLarge
    BackupError.UnsupportedVersion, BackupError.UnsupportedCurrency -> BackupOutcome.UnsupportedFormat
    else -> BackupOutcome.InvalidData
}
