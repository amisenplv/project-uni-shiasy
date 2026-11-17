package org.unishiasy.vehiclelog.ui.reminders

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
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
    private val savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoreInitialState(savedStateHandle))
    val state = mutableState.asStateFlow()

    fun todayDate(): LocalDate = JalaliDates.captureDefaultDate(clock)

    fun open(vehicle: VehicleEntity) {
        updateState {
            it.copy(vehicle = vehicle, draft = ReminderDraft(), showValidation = false, message = null, actionId = null)
        }
    }

    fun back() {
        if (!state.value.writing) updateState { it.copy(vehicle = null) }
    }

    fun retry() {
        updateState { it.copy(loadAttempt = it.loadAttempt + 1) }
    }

    suspend fun observe(vehicleId: String) {
        updateState { current ->
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
            updateState { it.copy(loadFailed = true) }
        }.collect { list ->
            updateState { it.copy(list = list, loadFailed = false) }
        }
    }

    fun edit(change: (ReminderDraft) -> ReminderDraft) {
        if (!state.value.writing) {
            updateState { it.copy(draft = change(it.draft), message = null, actionId = null) }
        }
    }

    fun save() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.writing) return
        val input = current.draft.input()
        if (input == null) {
            updateState { it.copy(showValidation = true, message = null, actionId = null) }
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
        updateState { it.copy(writing = true, message = null, actionId = actionId) }
        viewModelScope.launch {
            val result = try {
                operation()
            } catch (_: IllegalStateException) {
                ReminderWriteResult.StorageFailure
            }
            updateState {
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

    private inline fun updateState(transform: (RemindersUiState) -> RemindersUiState) {
        var nextState: RemindersUiState? = null
        mutableState.update { current ->
            transform(current).also { nextState = it }
        }
        nextState?.let(::persistState)
    }

    private fun persistState(state: RemindersUiState) {
        val handle = savedStateHandle ?: return
        val vehicle = state.vehicle
        handle[KEY_VEHICLE_PRESENT] = vehicle != null
        handle[KEY_VEHICLE_ID] = vehicle?.id
        handle[KEY_VEHICLE_KIND] = vehicle?.kind
        handle[KEY_VEHICLE_LABEL] = vehicle?.displayLabel
        handle[KEY_VEHICLE_CATALOG] = vehicle?.catalogKey
        handle[KEY_VEHICLE_YEAR] = vehicle?.productionYear
        handle[KEY_VEHICLE_CALENDAR] = vehicle?.productionYearCalendar
        handle[KEY_DRAFT_ID] = state.draft.id.toString()
        handle[KEY_DRAFT_TITLE] = state.draft.title
        handle[KEY_DRAFT_THRESHOLD] = state.draft.threshold.name
        handle[KEY_DRAFT_DATE] = state.draft.date
        handle[KEY_DRAFT_MILEAGE] = state.draft.mileage
        handle[KEY_SHOW_VALIDATION] = state.showValidation
    }

    companion object {
        private const val KEY_VEHICLE_PRESENT = "reminders_vm_vehicle_present"
        private const val KEY_VEHICLE_ID = "reminders_vm_vehicle_id"
        private const val KEY_VEHICLE_KIND = "reminders_vm_vehicle_kind"
        private const val KEY_VEHICLE_LABEL = "reminders_vm_vehicle_label"
        private const val KEY_VEHICLE_CATALOG = "reminders_vm_vehicle_catalog"
        private const val KEY_VEHICLE_YEAR = "reminders_vm_vehicle_year"
        private const val KEY_VEHICLE_CALENDAR = "reminders_vm_vehicle_calendar"
        private const val KEY_DRAFT_ID = "reminders_vm_draft_id"
        private const val KEY_DRAFT_TITLE = "reminders_vm_draft_title"
        private const val KEY_DRAFT_THRESHOLD = "reminders_vm_draft_threshold"
        private const val KEY_DRAFT_DATE = "reminders_vm_draft_date"
        private const val KEY_DRAFT_MILEAGE = "reminders_vm_draft_mileage"
        private const val KEY_SHOW_VALIDATION = "reminders_vm_show_validation"

        private fun restoreInitialState(handle: SavedStateHandle?): RemindersUiState {
            if (handle == null) return RemindersUiState()
            val vehicle = if (handle.get<Boolean>(KEY_VEHICLE_PRESENT) == true) {
                val id = handle.get<String>(KEY_VEHICLE_ID)
                val kind = handle.get<String>(KEY_VEHICLE_KIND)
                val label = handle.get<String>(KEY_VEHICLE_LABEL)
                if (id != null && kind != null && label != null) {
                    VehicleEntity(
                        id = id,
                        kind = kind,
                        displayLabel = label,
                        catalogKey = handle.get<String>(KEY_VEHICLE_CATALOG),
                        productionYear = handle.get<Int>(KEY_VEHICLE_YEAR),
                        productionYearCalendar = handle.get<String>(KEY_VEHICLE_CALENDAR),
                    )
                } else null
            } else null
            val draftId = handle.get<String>(KEY_DRAFT_ID)?.let { raw ->
                runCatching { UUID.fromString(raw) }.getOrNull()
            } ?: UUID.randomUUID()
            val threshold = handle.get<String>(KEY_DRAFT_THRESHOLD)?.let { raw ->
                ReminderThreshold.entries.firstOrNull { it.name == raw }
            } ?: ReminderThreshold.Date
            val draft = ReminderDraft(
                id = draftId,
                title = handle.get<String>(KEY_DRAFT_TITLE).orEmpty(),
                threshold = threshold,
                date = handle.get<String>(KEY_DRAFT_DATE).orEmpty(),
                mileage = handle.get<String>(KEY_DRAFT_MILEAGE).orEmpty(),
            )
            val showValidation = handle.get<Boolean>(KEY_SHOW_VALIDATION) == true
            return RemindersUiState(vehicle = vehicle, draft = draft, showValidation = showValidation)
        }
    }
}
