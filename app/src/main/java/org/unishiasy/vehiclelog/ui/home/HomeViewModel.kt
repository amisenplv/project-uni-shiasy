package org.unishiasy.vehiclelog.ui.home

import android.database.sqlite.SQLiteException
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.OdometerInput
import org.unishiasy.vehiclelog.data.VehicleMonthlyReport
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.OdometerReadingEntity
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.normalizeInputDigits
import org.unishiasy.vehiclelog.domain.withPersianDigits
import org.unishiasy.vehiclelog.ui.reminders.millisUntilTehranMidnight

data class OdometerDraft(
    val vehicleId: String,
    val id: UUID = UUID.randomUUID(),
    val editing: Boolean = false,
    val kilometers: String = "",
    val date: String = "",
    val saving: Boolean = false,
    val error: Int? = null,
    val kmError: Int? = null,
    val dateError: Int? = null,
    val pendingDeleteId: String? = null,
)

data class HomeUiState(
    val vehicles: List<VehicleEntity>? = null,
    val selectedVehicleId: String? = null,
    val selectedVehicleMileage: Long? = null,
    val mileageLoaded: Boolean = false,
    val listFailed: Boolean = false,
    val recentHistory: List<org.unishiasy.vehiclelog.data.local.HistoryEntry>? = null,
    val standaloneReadings: List<OdometerReadingEntity> = emptyList(),
    val odometerDraft: OdometerDraft? = null,
    val odometerFeedbackRes: Int? = null,
) {
    val selectedVehicle: VehicleEntity?
        get() = vehicles?.firstOrNull { it.id == selectedVehicleId }
            ?: vehicles?.firstOrNull()
}

class SelectedVehiclePreferences(context: android.content.Context) {
    private val prefs = context.getSharedPreferences("vehicle_log_selection", android.content.Context.MODE_PRIVATE)

    fun getSelectedVehicleId(): String? = prefs.getString(KEY_SELECTED_VEHICLE_ID, null)

    fun setSelectedVehicleId(vehicleId: String) {
        prefs.edit { putString(KEY_SELECTED_VEHICLE_ID, vehicleId) }
    }

    companion object {
        private const val KEY_SELECTED_VEHICLE_ID = "selected_vehicle_id"
    }
}

class HomeViewModel(
    private val vehicleRepository: VehicleRepository,
    private val ledgerRepository: LedgerRepository,
    coroutineScope: CoroutineScope? = null,
    initialSelectedVehicleId: String? = null,
    private val onPersistSelectedVehicleId: (String) -> Unit = {},
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState(selectedVehicleId = initialSelectedVehicleId))
    val state = mutableState.asStateFlow()
    private val mutableMonthlyReport = MutableStateFlow<Pair<String, VehicleMonthlyReport>?>(null)
    internal val monthlyReport = mutableMonthlyReport.asStateFlow()

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private var vehiclesJob: Job? = null
    private var mileageJob: Job? = null

    init {
        observeVehicles()
    }

    fun todayDate(): LocalDate = JalaliDates.captureDefaultDate(clock)

    internal val currentMonth = flow {
        while (true) {
            val now = clock.instant()
            emit(JalaliDates.monthOf(JalaliDates.captureDefaultDate(Clock.fixed(now, clock.zone))))
            delay(millisUntilTehranMidnight(now).coerceAtMost(3_600_000))
        }
    }.distinctUntilChanged()

    fun selectVehicle(vehicleId: String) {
        if (state.value.selectedVehicleId == vehicleId) return
        onPersistSelectedVehicleId(vehicleId)
        mutableState.update { it.withVehicleSelection(vehicleId) }
        observeMileageForSelected()
    }

    private fun HomeUiState.withVehicleSelection(vehicleId: String?): HomeUiState =
        if (selectedVehicleId == vehicleId) this
        else copy(
            selectedVehicleId = vehicleId,
            selectedVehicleMileage = null,
            mileageLoaded = false,
            recentHistory = null,
            standaloneReadings = emptyList(),
            odometerDraft = null,
            odometerFeedbackRes = null,
        )

    private inline fun updateSelectedVehicle(vehicleId: String, update: (HomeUiState) -> HomeUiState) {
        mutableState.update { current -> if (current.selectedVehicleId == vehicleId) update(current) else current }
    }

    fun openOdometerSheet() {
        val selectedId = state.value.selectedVehicle?.id ?: return
        val today = todayDate()
        val defaultJalali = JalaliDates.formatJalaliInput(today.toString()).orEmpty()
        mutableState.update {
            it.copy(
                odometerFeedbackRes = null,
                odometerDraft = OdometerDraft(
                    vehicleId = selectedId,
                    date = defaultJalali,
                ),
            )
        }
    }

    fun closeOdometerSheet() {
        val draft = state.value.odometerDraft ?: return
        if (draft.saving) return
        mutableState.update { it.copy(odometerDraft = null) }
    }

    fun dismissOdometerFeedback() {
        mutableState.update { it.copy(odometerFeedbackRes = null) }
    }

    fun changeOdometerKilometers(value: String) {
        mutableState.update { current ->
            val draft = current.odometerDraft ?: return@update current
            if (draft.saving) return@update current
            current.copy(
                odometerDraft = draft.copy(
                    kilometers = value,
                    kmError = null,
                    error = null,
                ),
            )
        }
    }

    fun changeOdometerDate(value: String) {
        mutableState.update { current ->
            val draft = current.odometerDraft ?: return@update current
            if (draft.saving) return@update current
            current.copy(
                odometerDraft = draft.copy(
                    date = value,
                    dateError = null,
                    error = null,
                ),
            )
        }
    }

    fun editStandaloneReading(reading: OdometerReadingEntity) {
        val uuid = runCatching { UUID.fromString(reading.id) }.getOrNull() ?: return
        val jalali = JalaliDates.formatJalaliInput(reading.occurredOn) ?: reading.occurredOn
        mutableState.update { current ->
            current.copy(
                odometerDraft = OdometerDraft(
                    vehicleId = reading.vehicleId,
                    id = uuid,
                    editing = true,
                    kilometers = reading.kilometers.toString().withPersianDigits(),
                    date = jalali,
                ),
            )
        }
    }

    fun requestDeleteStandaloneReading(readingId: String) {
        mutableState.update { current ->
            val draft = current.odometerDraft ?: return@update current
            if (draft.saving) return@update current
            current.copy(odometerDraft = draft.copy(pendingDeleteId = readingId, error = null))
        }
    }

    fun cancelDeleteStandaloneReading() {
        mutableState.update { current ->
            val draft = current.odometerDraft ?: return@update current
            if (draft.saving) return@update current
            current.copy(odometerDraft = draft.copy(pendingDeleteId = null))
        }
    }

    fun confirmDeleteStandaloneReading() {
        val current = state.value
        val draft = current.odometerDraft ?: return
        val deleteId = draft.pendingDeleteId ?: return
        if (draft.saving) return
        val uuid = runCatching { UUID.fromString(deleteId) }.getOrNull() ?: return
        mutableState.update { it.copy(odometerDraft = draft.copy(saving = true, pendingDeleteId = null, error = null)) }
        scope.launch {
            val result = ledgerRepository.deleteStandaloneReading(uuid)
            mutableState.update { st ->
                val curDraft = st.odometerDraft
                if (result == EntrySaveResult.Saved) {
                    val resetEditing = curDraft != null && curDraft.id == uuid
                    st.copy(
                        odometerFeedbackRes = R.string.odometer_deleted,
                        odometerDraft = if (resetEditing) {
                            OdometerDraft(
                                vehicleId = curDraft.vehicleId,
                                date = JalaliDates.formatJalaliInput(todayDate().toString()).orEmpty(),
                            )
                        } else {
                            curDraft?.copy(saving = false, pendingDeleteId = null)
                        },
                    )
                } else {
                    st.copy(
                        odometerDraft = curDraft?.copy(
                            saving = false,
                            error = R.string.ledger_storage_failure,
                        ),
                    )
                }
            }
        }
    }

    fun saveStandaloneOdometer() {
        val draft = state.value.odometerDraft ?: return
        if (draft.saving) return
        val normalizedKm = draft.kilometers.trim().normalizeInputDigits()
        val km = normalizedKm.takeIf { it.isNotEmpty() && it.all { ch -> ch in '0'..'9' } }?.toLongOrNull()
        val gregorian = JalaliDates.parseJalali(draft.date)
        val kmError = if (km == null) R.string.ledger_mileage_invalid else null
        val dateError = if (gregorian == null) R.string.ledger_date_invalid else null
        if (kmError != null || dateError != null || km == null || gregorian == null) {
            mutableState.update {
                it.copy(odometerDraft = draft.copy(kmError = kmError, dateError = dateError))
            }
            return
        }
        mutableState.update {
            it.copy(odometerDraft = draft.copy(saving = true, kmError = null, dateError = null, error = null))
        }
        scope.launch {
            val input = OdometerInput(draft.vehicleId, gregorian, km)
            val result = if (draft.editing) {
                ledgerRepository.updateStandaloneReading(draft.id, input)
            } else {
                ledgerRepository.createStandaloneReading(draft.id, input)
            }
            mutableState.update { current ->
                when (result) {
                    EntrySaveResult.Saved -> current.copy(
                        odometerDraft = null,
                        odometerFeedbackRes = R.string.odometer_saved,
                    )
                    EntrySaveResult.MileageConflict -> current.copy(
                        odometerDraft = current.odometerDraft?.copy(
                            saving = false,
                            kmError = R.string.ledger_mileage_conflict,
                            error = R.string.ledger_mileage_conflict,
                        ),
                    )
                    EntrySaveResult.InvalidKilometers -> current.copy(
                        odometerDraft = current.odometerDraft?.copy(
                            saving = false,
                            kmError = R.string.ledger_mileage_invalid,
                        ),
                    )
                    EntrySaveResult.InvalidOccurrenceDate -> current.copy(
                        odometerDraft = current.odometerDraft?.copy(
                            saving = false,
                            dateError = R.string.ledger_date_invalid,
                        ),
                    )
                    else -> current.copy(
                        odometerDraft = current.odometerDraft?.copy(
                            saving = false,
                            error = R.string.ledger_storage_failure,
                        ),
                    )
                }
            }
        }
    }

    fun retry() {
        mutableState.update { it.copy(listFailed = false, vehicles = null) }
        observeVehicles()
    }

    private fun observeVehicles() {
        vehiclesJob?.cancel()
        vehiclesJob = scope.launch {
            vehicleRepository.observeAll()
                .catch { failure ->
                    if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                    mutableState.update { it.copy(listFailed = true) }
                }
                .collect { list ->
                    var resolvedId: String? = null
                    mutableState.update { current ->
                        val selectedId = current.selectedVehicleId?.takeIf { id -> list.any { it.id == id } }
                            ?: list.firstOrNull()?.id
                        resolvedId = selectedId
                        current.withVehicleSelection(selectedId).copy(vehicles = list, listFailed = false)
                    }
                    resolvedId?.let(onPersistSelectedVehicleId)
                    observeMileageForSelected()
                }
        }
    }

    private fun observeMileageForSelected() {
        val selectedId = state.value.selectedVehicle?.id
        mileageJob?.cancel()
        mutableMonthlyReport.value = null
        if (selectedId == null) {
            mutableState.update {
                it.copy(
                    selectedVehicleMileage = null,
                    mileageLoaded = true,
                    recentHistory = emptyList(),
                    standaloneReadings = emptyList(),
                )
            }
            return
        }
        mileageJob = scope.launch {
            launch {
                ledgerRepository.observeCurrentMileage(selectedId)
                    .catch { failure ->
                        if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                        updateSelectedVehicle(selectedId) { it.copy(selectedVehicleMileage = null, mileageLoaded = true) }
                    }
                    .collect { mileage ->
                        updateSelectedVehicle(selectedId) { it.copy(selectedVehicleMileage = mileage, mileageLoaded = true) }
                    }
            }
            launch {
                ledgerRepository.observeHistoryForVehicle(selectedId)
                    .catch { failure ->
                        if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                        updateSelectedVehicle(selectedId) { it.copy(recentHistory = emptyList()) }
                    }
                    .collect { history ->
                        updateSelectedVehicle(selectedId) { it.copy(recentHistory = history.take(3)) }
                    }
            }
            launch {
                ledgerRepository.observeMonthlyReport(selectedId)
                    .catch { failure ->
                        if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                    }
                    .collect { report -> mutableMonthlyReport.value = selectedId to report }
            }
            launch {
                ledgerRepository.observeStandaloneReadings(selectedId)
                    .catch { failure ->
                        if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                        updateSelectedVehicle(selectedId) { it.copy(standaloneReadings = emptyList()) }
                    }
                    .collect { readings ->
                        updateSelectedVehicle(selectedId) { it.copy(standaloneReadings = readings) }
                    }
            }
        }
    }
}
