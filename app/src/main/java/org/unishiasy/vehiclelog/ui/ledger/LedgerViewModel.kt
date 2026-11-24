package org.unishiasy.vehiclelog.ui.ledger

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.R
import org.unishiasy.vehiclelog.data.EntrySaveResult
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliDates
import org.unishiasy.vehiclelog.domain.JalaliMonth

internal data class LedgerUiState(
    val vehicle: VehicleEntity? = null,
    val entries: List<HistoryEntry>? = null,
    val latestMileage: Long? = null,
    val listFailed: Boolean = false,
    val openingEntry: Boolean = false,
    val draft: EntryDraft? = null,
    val returnToHome: Boolean = false,
    val draftChanged: Boolean = false,
    val fieldErrors: Map<EntryField, Int> = emptyMap(),
    val saving: Boolean = false,
    val result: EntrySaveResult? = null,
    val entryPendingDeletion: String? = null,
    val deleting: Boolean = false,
    val lastActionWasDelete: Boolean = false,
    val dashboard: MonthlyDashboardState? = null,
    val inspectingEntryId: String? = null,
    val toolsOpen: Boolean = false,
    val tripCalculatorOpen: Boolean = false,
) {
    val recent: List<HistoryEntry>? get() = entries
}

internal class LedgerViewModel(
    private val repository: LedgerRepository,
    private val clock: Clock = Clock.systemUTC(),
    private val savedStateHandle: SavedStateHandle? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoreInitialState(savedStateHandle))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var dashboardObservation: Job? = null

    init {
        val restored = mutableState.value
        val vehicle = restored.vehicle
        if (vehicle != null) {
            observe(vehicle.id)
            if (restored.dashboard != null) {
                observeDashboard(vehicle.id)
            }
        }
    }

    fun todayDate(): LocalDate = JalaliDates.captureDefaultDate(clock)

    fun openVehicle(vehicle: VehicleEntity) {
        if (state.value.vehicle != null) return
        setState(LedgerUiState(vehicle = vehicle))
        observe(vehicle.id)
    }

    fun openQuickAdd(vehicle: VehicleEntity, initialCategory: EntryCategory = EntryCategory.Fuel) {
        val current = state.value
        if (current.vehicle != null && current.vehicle.id != vehicle.id ||
            current.draft != null || current.dashboard != null || current.openingEntry ||
            current.inspectingEntryId != null || current.entryPendingDeletion != null || current.deleting
        ) return
        setState(
            current.copy(
                vehicle = vehicle,
                draft = EntryDraft.create(vehicle.id, clock).copy(category = initialCategory),
                returnToHome = current.vehicle == null,
                result = null,
                lastActionWasDelete = false,
            ),
        )
        if (current.vehicle == null) observe(vehicle.id)
    }

    fun openDashboardForVehicle(vehicle: VehicleEntity, month: JalaliMonth? = JalaliDates.monthOf(todayDate())) {
        if (state.value.vehicle != null) return
        val currentToolsOpen = state.value.toolsOpen
        setState(
            LedgerUiState(
                vehicle = vehicle,
                dashboard = MonthlyDashboardState(currentMonth = month),
                returnToHome = true,
                toolsOpen = currentToolsOpen,
            )
        )
        observe(vehicle.id)
        observeDashboard(vehicle.id)
    }

    fun openTools() {
        val current = state.value
        if (current.vehicle != null) {
            observation?.cancel()
            dashboardObservation?.cancel()
        }
        setState(LedgerUiState(toolsOpen = true))
    }

    fun openTripCalculator() {
        updateState { it.copy(toolsOpen = true, tripCalculatorOpen = true) }
    }

    fun closeTripCalculator() {
        updateState { it.copy(tripCalculatorOpen = false) }
    }

    fun closeTools() {
        updateState { it.copy(toolsOpen = false, tripCalculatorOpen = false) }
    }

    fun openEntryFromHome(vehicle: VehicleEntity, entryId: String) {
        if (state.value.vehicle != null) return
        setState(LedgerUiState(vehicle = vehicle, inspectingEntryId = entryId))
        observe(vehicle.id)
    }

    fun inspectEntry(id: String) {
        val current = state.value
        if (current.draft != null || current.openingEntry || current.deleting) return
        updateState { it.copy(inspectingEntryId = id, result = null, lastActionWasDelete = false) }
    }

    fun dismissEntryDetail() {
        updateState { it.copy(inspectingEntryId = null) }
    }

    fun editFromDetail(id: String) {
        updateState { it.copy(inspectingEntryId = null) }
        editEntry(id)
    }

    fun deleteFromDetail(id: String) {
        updateState { it.copy(inspectingEntryId = null) }
        requestDelete(id)
    }

    fun retryList() {
        val vehicle = state.value.vehicle ?: return
        updateState { it.copy(listFailed = false, entries = null) }
        observe(vehicle.id)
    }

    private fun observe(vehicleId: String) {
        observation?.cancel()
        observation = viewModelScope.launch {
            combine(repository.observeHistoryForVehicle(vehicleId), repository.observeCurrentMileage(vehicleId), ::Pair)
                .catch { failure ->
                    if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                    updateState { it.copy(listFailed = true) }
                }
                .collect { (entries, mileage) ->
                    updateState { it.copy(entries = entries, latestMileage = mileage) }
                }
        }
    }

    fun openDashboard() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.dashboard != null || current.draft != null || current.openingEntry ||
            current.entryPendingDeletion != null || current.deleting
        ) return
        setState(current.copy(dashboard = MonthlyDashboardState.open(clock)))
        observeDashboard(vehicle.id)
    }

    fun selectDashboardMonth(month: JalaliMonth) {
        updateState { it.copy(dashboard = it.dashboard?.select(month)) }
    }

    fun retryDashboard() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        val dashboard = current.dashboard?.takeIf { it.failed } ?: return
        setState(current.copy(dashboard = dashboard.copy(report = null, failed = false)))
        observeDashboard(vehicle.id)
    }

    private fun observeDashboard(vehicleId: String) {
        dashboardObservation?.cancel()
        dashboardObservation = viewModelScope.launch {
            repository.observeMonthlyReport(vehicleId)
                .catch { failure ->
                    if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                    updateState { it.copy(dashboard = it.dashboard?.copy(failed = true)) }
                }
                .collect { report ->
                    updateState { it.copy(dashboard = it.dashboard?.copy(report = report, failed = false)) }
                }
        }
    }

    fun newEntry() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.draft != null || current.openingEntry || current.deleting) return
        setState(current.copy(draft = EntryDraft.create(vehicle.id, clock), result = null, lastActionWasDelete = false))
    }

    fun editEntry(id: String) {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.draft != null || current.openingEntry || current.deleting) return
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            updateState { it.copy(result = EntrySaveResult.NotFound, lastActionWasDelete = false) }
            return
        }
        updateState { it.copy(openingEntry = true, result = null, lastActionWasDelete = false) }
        viewModelScope.launch {
            val (draft, failure) = try {
                val saved = repository.findWithReading(uuid)?.takeIf { it.entry.vehicleId == vehicle.id }
                val draft = saved?.let(EntryDraft::edit)
                draft to EntrySaveResult.NotFound.takeIf { draft == null }
            } catch (_: SQLiteException) {
                null to EntrySaveResult.StorageFailure
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: IllegalStateException) {
                null to EntrySaveResult.StorageFailure
            }
            updateState { it.copy(draft = draft, openingEntry = false, result = failure) }
        }
    }

    fun requestDelete(id: String) {
        val current = state.value
        if (current.saving || current.openingEntry || current.deleting) return
        updateState { it.copy(entryPendingDeletion = id, result = null) }
    }

    fun cancelDelete() {
        if (state.value.deleting) return
        updateState { it.copy(entryPendingDeletion = null) }
    }

    fun confirmDelete() {
        val current = state.value
        val entryId = current.entryPendingDeletion ?: return
        val vehicle = current.vehicle ?: return
        if (current.deleting) return
        val uuid = runCatching { UUID.fromString(entryId) }.getOrNull()
        if (uuid == null) {
            updateState {
                it.copy(
                    entryPendingDeletion = null,
                    result = EntrySaveResult.NotFound,
                    lastActionWasDelete = true,
                )
            }
            return
        }
        updateState { it.copy(deleting = true) }
        viewModelScope.launch {
            val result = repository.deleteEntry(vehicle.id, uuid)
            updateState {
                it.copy(
                    deleting = false,
                    entryPendingDeletion = null,
                    draft = if (result == EntrySaveResult.Saved && it.draft?.id == uuid) null else it.draft,
                    result = result,
                    lastActionWasDelete = true,
                )
            }
        }
    }

    fun back() {
        val current = state.value
        if (current.tripCalculatorOpen) {
            closeTripCalculator()
            return
        }
        if (current.dashboard != null) {
            dashboardObservation?.cancel()
            if (current.returnToHome) {
                observation?.cancel()
                setState(if (current.toolsOpen) LedgerUiState(toolsOpen = true) else LedgerUiState())
            } else {
                setState(current.copy(dashboard = null))
            }
            return
        }
        if (current.toolsOpen) {
            closeTools()
            return
        }
        if (current.saving || current.openingEntry || current.deleting) return
        if (current.entryPendingDeletion != null) {
            cancelDelete()
            return
        }
        if (current.inspectingEntryId != null) {
            dismissEntryDetail()
            return
        }
        if (current.draft != null && !current.returnToHome) {
            setState(
                current.copy(
                    draft = null,
                    draftChanged = false,
                    result = null,
                    fieldErrors = emptyMap(),
                    lastActionWasDelete = false,
                ),
            )
        } else {
            observation?.cancel()
            setState(LedgerUiState())
        }
    }

    fun changeCategory(category: EntryCategory) = change(setOf(EntryField.Amount, EntryField.Volume)) {
        it.copy(category = category)
    }

    fun changeField(field: EntryField, value: String) = change(setOf(field)) {
        when (field) {
            EntryField.Amount -> it.copy(amount = value)
            EntryField.Date -> it.copy(date = value)
            EntryField.Volume -> it.copy(volume = value)
            EntryField.Mileage -> it.copy(mileage = value)
            EntryField.Notes -> it.copy(notes = value)
            EntryField.StationName -> it.copy(stationName = value)
        }
    }

    fun changeFullTank(isFullTank: Boolean) = change(emptySet()) {
        it.copy(isFullTank = isFullTank)
    }

    fun toggleMileageRemoval() = change(setOf(EntryField.Mileage)) {
        it.copy(omitMileage = !it.omitMileage)
    }

    private fun change(fields: Set<EntryField>, transform: (EntryDraft) -> EntryDraft) {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving) return
        val mileageChanged = EntryField.Mileage in fields || EntryField.Date in fields
        val clearsConflict = mileageChanged && current.result == EntrySaveResult.MileageConflict
        val changedDraft = transform(draft)
        val remainingErrors = current.fieldErrors - fields
        setState(
            current.copy(
                draft = changedDraft,
                draftChanged = current.draftChanged || draft != changedDraft,
                fieldErrors = if (clearsConflict) remainingErrors - EntryField.Mileage else remainingErrors,
                result = current.result.takeIf { it == EntrySaveResult.MileageConflict && !mileageChanged },
            )
        )
    }

    fun saveWithoutMileage() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving || current.result != EntrySaveResult.MileageConflict) return
        setState(current.copy(draft = draft.copy(omitMileage = true), draftChanged = true))
        save()
    }

    fun save() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving) return
        when (val validation = draft.validate()) {
            is DraftValidation.Invalid -> setState(current.copy(fieldErrors = validation.errors))
            is DraftValidation.Valid -> {
                setState(current.copy(saving = true, fieldErrors = emptyMap(), result = null, lastActionWasDelete = false))
                viewModelScope.launch {
                    val result = if (draft.editing) {
                        repository.update(draft.id, validation.input, validation.readingEdit)
                    } else {
                        repository.create(draft.id, draft.createdAt, validation.input, validation.kilometers)
                    }
                    updateState {
                        it.copy(
                            draft = it.draft.takeUnless { result == EntrySaveResult.Saved },
                            returnToHome = it.returnToHome && result != EntrySaveResult.Saved,
                            draftChanged = it.draftChanged && result != EntrySaveResult.Saved,
                            saving = false,
                            result = result,
                            fieldErrors = result.fieldErrors(),
                            lastActionWasDelete = false,
                        )
                    }
                }
            }
        }
    }

    private fun setState(next: LedgerUiState) {
        mutableState.value = next
        persistState(next)
    }

    private inline fun updateState(transform: (LedgerUiState) -> LedgerUiState) {
        var nextState: LedgerUiState? = null
        mutableState.update { current ->
            transform(current).also { nextState = it }
        }
        nextState?.let(::persistState)
    }

    private fun persistState(state: LedgerUiState) {
        val handle = savedStateHandle ?: return
        val vehicle = state.vehicle
        handle[KEY_VEHICLE_PRESENT] = vehicle != null
        handle[KEY_VEHICLE_ID] = vehicle?.id
        handle[KEY_VEHICLE_KIND] = vehicle?.kind
        handle[KEY_VEHICLE_LABEL] = vehicle?.displayLabel
        handle[KEY_VEHICLE_CATALOG] = vehicle?.catalogKey
        handle[KEY_VEHICLE_YEAR] = vehicle?.productionYear
        handle[KEY_VEHICLE_CALENDAR] = vehicle?.productionYearCalendar

        val draft = state.draft
        handle[KEY_DRAFT_PRESENT] = draft != null
        handle[KEY_RETURN_TO_HOME] = state.returnToHome
        handle[KEY_DRAFT_CHANGED] = state.draftChanged
        handle[KEY_DRAFT_VEHICLE_ID] = draft?.vehicleId
        handle[KEY_DRAFT_ID] = draft?.id?.toString()
        handle[KEY_DRAFT_CREATED_AT] = draft?.createdAt
        handle[KEY_DRAFT_EDITING] = draft?.editing
        handle[KEY_DRAFT_CATEGORY] = draft?.category?.key
        handle[KEY_DRAFT_AMOUNT] = draft?.amount
        handle[KEY_DRAFT_DATE] = draft?.date
        handle[KEY_DRAFT_ORIGINAL_DATE] = draft?.originalGregorianDate
        handle[KEY_DRAFT_VOLUME] = draft?.volume
        handle[KEY_DRAFT_MILEAGE] = draft?.mileage
        handle[KEY_DRAFT_ORIGINAL_MILEAGE] = draft?.originalMileage
        handle[KEY_DRAFT_OMIT_MILEAGE] = draft?.omitMileage
        handle[KEY_DRAFT_NOTES] = draft?.notes
        handle[KEY_DRAFT_STATION_NAME] = draft?.stationName
        handle[KEY_DRAFT_IS_FULL_TANK] = draft?.isFullTank

        val dashboard = state.dashboard
        handle[KEY_DASHBOARD_OPEN] = dashboard != null
        handle[KEY_DASHBOARD_CUR_YEAR] = dashboard?.currentMonth?.year
        handle[KEY_DASHBOARD_CUR_MONTH] = dashboard?.currentMonth?.month
        handle[KEY_DASHBOARD_SEL_YEAR] = dashboard?.selectedMonth?.year
        handle[KEY_DASHBOARD_SEL_MONTH] = dashboard?.selectedMonth?.month

        handle[KEY_INSPECTING_ENTRY_ID] = state.inspectingEntryId
        handle[KEY_PENDING_DELETION_ID] = state.entryPendingDeletion
        handle[KEY_TOOLS_OPEN] = state.toolsOpen
        handle[KEY_TRIP_CALCULATOR_OPEN] = state.tripCalculatorOpen
    }

    companion object {
        private const val KEY_TOOLS_OPEN = "ledger_vm_tools_open"
        private const val KEY_TRIP_CALCULATOR_OPEN = "ledger_vm_trip_calculator_open"
        private const val KEY_VEHICLE_PRESENT = "ledger_vm_vehicle_present"
        private const val KEY_VEHICLE_ID = "ledger_vm_vehicle_id"
        private const val KEY_VEHICLE_KIND = "ledger_vm_vehicle_kind"
        private const val KEY_VEHICLE_LABEL = "ledger_vm_vehicle_label"
        private const val KEY_VEHICLE_CATALOG = "ledger_vm_vehicle_catalog"
        private const val KEY_VEHICLE_YEAR = "ledger_vm_vehicle_year"
        private const val KEY_VEHICLE_CALENDAR = "ledger_vm_vehicle_calendar"

        private const val KEY_DRAFT_CHANGED = "ledger_vm_draft_changed"
        private const val KEY_RETURN_TO_HOME = "ledger_vm_return_to_home"
        private const val KEY_DRAFT_PRESENT = "ledger_vm_draft_present"
        private const val KEY_DRAFT_VEHICLE_ID = "ledger_vm_draft_vehicle_id"
        private const val KEY_DRAFT_ID = "ledger_vm_draft_id"
        private const val KEY_DRAFT_CREATED_AT = "ledger_vm_draft_created_at"
        private const val KEY_DRAFT_EDITING = "ledger_vm_draft_editing"
        private const val KEY_DRAFT_CATEGORY = "ledger_vm_draft_category"
        private const val KEY_DRAFT_AMOUNT = "ledger_vm_draft_amount"
        private const val KEY_DRAFT_DATE = "ledger_vm_draft_date"
        private const val KEY_DRAFT_ORIGINAL_DATE = "ledger_vm_draft_original_date"
        private const val KEY_DRAFT_VOLUME = "ledger_vm_draft_volume"
        private const val KEY_DRAFT_MILEAGE = "ledger_vm_draft_mileage"
        private const val KEY_DRAFT_ORIGINAL_MILEAGE = "ledger_vm_draft_original_mileage"
        private const val KEY_DRAFT_OMIT_MILEAGE = "ledger_vm_draft_omit_mileage"
        private const val KEY_DRAFT_NOTES = "ledger_vm_draft_notes"
        private const val KEY_DRAFT_STATION_NAME = "ledger_vm_draft_station_name"
        private const val KEY_DRAFT_IS_FULL_TANK = "ledger_vm_draft_is_full_tank"

        private const val KEY_DASHBOARD_OPEN = "ledger_vm_dashboard_open"
        private const val KEY_DASHBOARD_CUR_YEAR = "ledger_vm_dashboard_cur_year"
        private const val KEY_DASHBOARD_CUR_MONTH = "ledger_vm_dashboard_cur_month"
        private const val KEY_DASHBOARD_SEL_YEAR = "ledger_vm_dashboard_sel_year"
        private const val KEY_DASHBOARD_SEL_MONTH = "ledger_vm_dashboard_sel_month"
        private const val KEY_INSPECTING_ENTRY_ID = "ledger_vm_inspecting_entry_id"
        private const val KEY_PENDING_DELETION_ID = "ledger_vm_pending_deletion_id"

        private fun restoreInitialState(handle: SavedStateHandle?): LedgerUiState {
            if (handle == null) return LedgerUiState()
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
            val toolsOpen = handle.get<Boolean>(KEY_TOOLS_OPEN) == true
            val tripCalculatorOpen = handle.get<Boolean>(KEY_TRIP_CALCULATOR_OPEN) == true
            if (vehicle == null) return LedgerUiState(toolsOpen = toolsOpen, tripCalculatorOpen = tripCalculatorOpen)

            val draft = if (handle.get<Boolean>(KEY_DRAFT_PRESENT) == true) {
                val draftVehicleId = handle.get<String>(KEY_DRAFT_VEHICLE_ID)
                val draftId = handle.get<String>(KEY_DRAFT_ID)?.let { raw ->
                    runCatching { UUID.fromString(raw) }.getOrNull()
                }
                val createdAt = handle.get<Long>(KEY_DRAFT_CREATED_AT)
                val categoryKey = handle.get<String>(KEY_DRAFT_CATEGORY)
                val category = EntryCategory.entries.firstOrNull { it.key == categoryKey }
                if (draftVehicleId != null && draftId != null && createdAt != null && category != null) {
                    EntryDraft(
                        vehicleId = draftVehicleId,
                        id = draftId,
                        createdAt = createdAt,
                        editing = handle.get<Boolean>(KEY_DRAFT_EDITING) == true,
                        category = category,
                        amount = handle.get<String>(KEY_DRAFT_AMOUNT).orEmpty(),
                        date = handle.get<String>(KEY_DRAFT_DATE).orEmpty(),
                        originalGregorianDate = handle.get<String>(KEY_DRAFT_ORIGINAL_DATE),
                        volume = handle.get<String>(KEY_DRAFT_VOLUME).orEmpty(),
                        mileage = handle.get<String>(KEY_DRAFT_MILEAGE).orEmpty(),
                        originalMileage = handle.get<Long>(KEY_DRAFT_ORIGINAL_MILEAGE),
                        omitMileage = handle.get<Boolean>(KEY_DRAFT_OMIT_MILEAGE) == true,
                        notes = handle.get<String>(KEY_DRAFT_NOTES).orEmpty(),
                        stationName = handle.get<String>(KEY_DRAFT_STATION_NAME).orEmpty(),
                        isFullTank = handle.get<Boolean>(KEY_DRAFT_IS_FULL_TANK) == true,
                    )
                } else null
            } else null

            val dashboard = if (handle.get<Boolean>(KEY_DASHBOARD_OPEN) == true) {
                val curYear = handle.get<Int>(KEY_DASHBOARD_CUR_YEAR)
                val curMonth = handle.get<Int>(KEY_DASHBOARD_CUR_MONTH)
                val selYear = handle.get<Int>(KEY_DASHBOARD_SEL_YEAR)
                val selMonth = handle.get<Int>(KEY_DASHBOARD_SEL_MONTH)
                val current = if (curYear != null && curMonth != null) JalaliMonth(curYear, curMonth) else null
                val selected = if (selYear != null && selMonth != null) JalaliMonth(selYear, selMonth) else current
                MonthlyDashboardState(currentMonth = current, selectedMonth = selected)
            } else null

            return LedgerUiState(
                vehicle = vehicle,
                draft = draft,
                returnToHome = (draft != null || dashboard != null) && handle.get<Boolean>(KEY_RETURN_TO_HOME) == true,
                draftChanged = draft != null && handle.get<Boolean>(KEY_DRAFT_CHANGED) == true,
                dashboard = dashboard,
                inspectingEntryId = handle.get<String>(KEY_INSPECTING_ENTRY_ID),
                entryPendingDeletion = handle.get<String>(KEY_PENDING_DELETION_ID),
                toolsOpen = toolsOpen,
                tripCalculatorOpen = tripCalculatorOpen,
            )
        }
    }
}

private fun EntrySaveResult.fieldErrors(): Map<EntryField, Int> = when (this) {
    EntrySaveResult.InvalidOccurrenceDate -> mapOf(EntryField.Date to R.string.ledger_date_invalid)
    EntrySaveResult.InvalidAmount -> mapOf(EntryField.Amount to R.string.ledger_amount_invalid)
    EntrySaveResult.InvalidFuelVolume -> mapOf(EntryField.Volume to R.string.ledger_volume_invalid)
    EntrySaveResult.InvalidKilometers -> mapOf(EntryField.Mileage to R.string.ledger_mileage_invalid)
    EntrySaveResult.MileageConflict -> mapOf(EntryField.Mileage to R.string.ledger_mileage_conflict)
    else -> emptyMap()
}
