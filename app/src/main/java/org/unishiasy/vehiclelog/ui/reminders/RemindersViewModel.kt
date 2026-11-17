package org.unishiasy.vehiclelog.ui.reminders

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.ReminderRepository
import org.unishiasy.vehiclelog.data.ReminderWriteResult
import org.unishiasy.vehiclelog.data.isDue
import org.unishiasy.vehiclelog.data.local.ReminderEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates

internal data class ReminderList(
    val rows: List<ReminderEntity>,
    val due: List<ReminderEntity>,
    val mileage: Long?,
)

internal data class RemindersUiState(
    val vehicle: VehicleEntity? = null,
    val observedVehicleId: String? = null,
    val list: ReminderList? = null,
    val loadFailed: Boolean = false,
    val loadAttempt: Int = 0,
    val draft: ReminderDraft = ReminderDraft(),
    val showValidation: Boolean = false,
    val writing: Boolean = false,
    val message: ReminderMessage? = null,
    val actionId: UUID? = null,
)

internal enum class ReminderMessage { Created, Completed, Canceled, Failed, NoActiveReminder }

internal class RemindersViewModel(
    private val repository: ReminderRepository,
    private val ledger: LedgerRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(RemindersUiState())
    val state = mutableState.asStateFlow()

    fun open(vehicle: VehicleEntity) {
        mutableState.update {
            it.copy(vehicle = vehicle, draft = ReminderDraft(), showValidation = false, message = null, actionId = null)
        }
    }

    fun back() {
        if (!state.value.writing) mutableState.update { it.copy(vehicle = null) }
    }

    fun retry() {
        mutableState.update { it.copy(loadAttempt = it.loadAttempt + 1) }
    }

    suspend fun observe(vehicleId: String) {
        mutableState.update { current ->
            current.copy(
                observedVehicleId = vehicleId,
                list = current.list.takeIf { current.observedVehicleId == vehicleId },
                loadFailed = false,
            )
        }
        val dates = flow {
            while (true) {
                val now = clock.instant()
                emit(JalaliDates.captureDefaultDate(Clock.fixed(now, clock.zone)))
                // Recheck periodically to bound staleness after manual wall-clock changes.
                delay(millisUntilTehranMidnight(now).coerceAtMost(3_600_000))
            }
        }
        combine(repository.observeForVehicle(vehicleId), ledger.observeCurrentMileage(vehicleId), dates) { rows, mileage, today ->
            ReminderList(rows, rows.filter { it.isDue(today, mileage) }, mileage)
        }.catch { failure ->
            if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
            mutableState.update { it.copy(loadFailed = true) }
        }.collect { list ->
            mutableState.update { it.copy(list = list, loadFailed = false) }
        }
    }

    fun edit(change: (ReminderDraft) -> ReminderDraft) {
        if (!state.value.writing) {
            mutableState.update { it.copy(draft = change(it.draft), message = null, actionId = null) }
        }
    }

    fun save() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.writing) return
        val input = current.draft.input()
        if (input == null) {
            mutableState.update { it.copy(showValidation = true, message = null, actionId = null) }
            return
        }
        write(ReminderMessage.Created) { repository.create(vehicle.id, current.draft.id, input) }
    }

    fun finish(id: UUID, complete: Boolean) {
        val vehicle = state.value.vehicle ?: return
        write(if (complete) ReminderMessage.Completed else ReminderMessage.Canceled, id) {
            if (complete) repository.complete(vehicle.id, id) else repository.cancel(vehicle.id, id)
        }
    }

    private fun write(
        success: ReminderMessage,
        actionId: UUID? = null,
        operation: suspend () -> ReminderWriteResult,
    ) {
        if (state.value.writing) return
        mutableState.update { it.copy(writing = true, message = null, actionId = actionId) }
        viewModelScope.launch {
            val result = try {
                operation()
            } catch (_: IllegalStateException) {
                ReminderWriteResult.StorageFailure
            }
            mutableState.update {
                val saved = result == ReminderWriteResult.Saved
                it.copy(
                    writing = false,
                    message = when (result) {
                        ReminderWriteResult.Saved -> success
                        ReminderWriteResult.NoActiveReminder -> ReminderMessage.NoActiveReminder
                        else -> ReminderMessage.Failed
                    },
                    draft = if (saved && success == ReminderMessage.Created) ReminderDraft() else it.draft,
                    showValidation = it.showValidation && !(saved && success == ReminderMessage.Created),
                )
            }
        }
    }
}
