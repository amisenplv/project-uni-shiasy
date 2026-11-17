package org.unishiasy.vehiclelog.data

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteException
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleDatabase
import org.unishiasy.vehiclelog.domain.JalaliDates

enum class ReminderWriteResult {
    Saved,
    InvalidInput,
    NoActiveReminder,
    ConstraintViolation,
    StorageFailure,
}

class ReminderRepository internal constructor(
    database: VehicleDatabase,
    private val ledgerRepository: LedgerRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val onDateCreated: () -> Unit = {},
    private val finishWithCleanup: suspend (UUID, suspend () -> Int) -> Int = { _, finish -> finish() },
) {
    private val dao = database.reminderDao()

    fun observeForVehicle(vehicleId: String): Flow<List<ReminderEntity>> = dao.observeForVehicle(vehicleId)

    internal suspend fun activeDateReminders(): List<ReminderEntity> = dao.activeDateReminders()

    // Recollect on resume or a Tehran date change to refresh date-only due status.
    fun observeDue(vehicleId: String): Flow<List<ReminderEntity>> =
        combine(observeForVehicle(vehicleId), ledgerRepository.observeCurrentMileage(vehicleId)) { reminders, mileage ->
            val today = JalaliDates.captureDefaultDate(clock)
            reminders.filter { it.isDue(today, mileage) }
        }

    suspend fun create(vehicleId: String, id: UUID, input: ReminderInput): ReminderWriteResult {
        if (!input.isValid()) return ReminderWriteResult.InvalidInput
        val result = write {
            dao.insert(
                ReminderEntity(id.toString(), vehicleId, input.title.trim(), input.dueOn, input.dueKilometers, ReminderState.Active.key),
            )
            ReminderWriteResult.Saved
        }
        if (result == ReminderWriteResult.Saved && input.dueOn != null) onDateCreated()
        return result
    }

    suspend fun complete(vehicleId: String, id: UUID): ReminderWriteResult =
        finish(vehicleId, id, ReminderState.Completed)

    suspend fun cancel(vehicleId: String, id: UUID): ReminderWriteResult =
        finish(vehicleId, id, ReminderState.Canceled)

    private suspend fun finish(vehicleId: String, id: UUID, state: ReminderState): ReminderWriteResult = write {
        val changed = finishWithCleanup(id) { dao.finish(vehicleId, id.toString(), state.key) }
        if (changed == 1) ReminderWriteResult.Saved
        else ReminderWriteResult.NoActiveReminder
    }

    private suspend fun write(operation: suspend () -> ReminderWriteResult): ReminderWriteResult = try {
        operation()
    } catch (_: SQLiteConstraintException) {
        ReminderWriteResult.ConstraintViolation
    } catch (_: SQLiteException) {
        ReminderWriteResult.StorageFailure
    }
}
