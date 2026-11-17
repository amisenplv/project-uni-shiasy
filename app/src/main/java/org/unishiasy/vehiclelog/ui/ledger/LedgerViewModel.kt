package org.unishiasy.vehiclelog.ui.ledger

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
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
import org.unishiasy.vehiclelog.data.local.EntryEntity
import org.unishiasy.vehiclelog.data.local.HistoryEntry
import org.unishiasy.vehiclelog.data.local.VehicleEntity
import org.unishiasy.vehiclelog.domain.JalaliMonth

internal data class LedgerUiState(
    val vehicle: VehicleEntity? = null,
    val entries: List<HistoryEntry>? = null,
    val latestMileage: Long? = null,
    val listFailed: Boolean = false,
    val openingEntry: Boolean = false,
    val draft: EntryDraft? = null,
    val fieldErrors: Map<EntryField, Int> = emptyMap(),
    val saving: Boolean = false,
    val result: EntrySaveResult? = null,
    val entryPendingDeletion: String? = null,
    val deleting: Boolean = false,
    val lastActionWasDelete: Boolean = false,
    val dashboard: MonthlyDashboardState? = null,
) {
    val recent: List<HistoryEntry>? get() = entries
}

internal class LedgerViewModel(
    private val repository: LedgerRepository,
    private val clock: Clock = Clock.systemUTC(),
) : ViewModel() {
    private val mutableState = MutableStateFlow(LedgerUiState())
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    private var dashboardObservation: Job? = null

    fun openVehicle(vehicle: VehicleEntity) {
        if (state.value.vehicle != null) return
        mutableState.value = LedgerUiState(vehicle = vehicle)
        observe(vehicle.id)
    }

    fun openQuickAdd(vehicle: VehicleEntity, initialCategory: EntryCategory = EntryCategory.Fuel) {
        if (state.value.vehicle != null) return
        mutableState.value = LedgerUiState(
            vehicle = vehicle,
            draft = EntryDraft.create(vehicle.id, clock).copy(category = initialCategory),
        )
        observe(vehicle.id)
    }

    fun openDashboardForVehicle(vehicle: VehicleEntity) {
        if (state.value.vehicle != null) return
        mutableState.value = LedgerUiState(
            vehicle = vehicle,
            dashboard = MonthlyDashboardState.open(clock),
        )
        observe(vehicle.id)
        observeDashboard(vehicle.id)
    }

    fun retryList() {
        val vehicle = state.value.vehicle ?: return
        mutableState.update { it.copy(listFailed = false, entries = null) }
        observe(vehicle.id)
    }

    private fun observe(vehicleId: String) {
        observation?.cancel()
        observation = viewModelScope.launch {
            combine(repository.observeHistoryForVehicle(vehicleId), repository.observeCurrentMileage(vehicleId), ::Pair)
                .catch { failure ->
                    if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                    mutableState.update { it.copy(listFailed = true) }
                }
                .collect { (entries, mileage) ->
                    mutableState.update { it.copy(entries = entries, latestMileage = mileage) }
                }
        }
    }

    fun openDashboard() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.dashboard != null || current.draft != null || current.openingEntry ||
            current.entryPendingDeletion != null || current.deleting
        ) return
        mutableState.value = current.copy(dashboard = MonthlyDashboardState.open(clock))
        observeDashboard(vehicle.id)
    }

    fun selectDashboardMonth(month: JalaliMonth) {
        mutableState.update { it.copy(dashboard = it.dashboard?.select(month)) }
    }

    fun retryDashboard() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        val dashboard = current.dashboard?.takeIf { it.failed } ?: return
        mutableState.value = current.copy(dashboard = dashboard.copy(report = null, failed = false))
        observeDashboard(vehicle.id)
    }

    private fun observeDashboard(vehicleId: String) {
        dashboardObservation?.cancel()
        dashboardObservation = viewModelScope.launch {
            repository.observeMonthlyReport(vehicleId)
                .catch { failure ->
                    if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                    mutableState.update { it.copy(dashboard = it.dashboard?.copy(failed = true)) }
                }
                .collect { report ->
                    mutableState.update { it.copy(dashboard = it.dashboard?.copy(report = report, failed = false)) }
                }
        }
    }

    fun newEntry() {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.draft != null || current.openingEntry || current.deleting) return
        mutableState.value = current.copy(draft = EntryDraft.create(vehicle.id, clock), result = null, lastActionWasDelete = false)
    }

    fun editEntry(id: String) {
        val current = state.value
        val vehicle = current.vehicle ?: return
        if (current.draft != null || current.openingEntry || current.deleting) return
        val uuid = runCatching { UUID.fromString(id) }.getOrNull()
        if (uuid == null) {
            mutableState.update { it.copy(result = EntrySaveResult.NotFound, lastActionWasDelete = false) }
            return
        }
        mutableState.update { it.copy(openingEntry = true, result = null, lastActionWasDelete = false) }
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
            mutableState.update { it.copy(draft = draft, openingEntry = false, result = failure) }
        }
    }

    fun requestDelete(id: String) {
        val current = state.value
        if (current.saving || current.openingEntry || current.deleting) return
        mutableState.update { it.copy(entryPendingDeletion = id, result = null) }
    }

    fun cancelDelete() {
        if (state.value.deleting) return
        mutableState.update { it.copy(entryPendingDeletion = null) }
    }

    fun confirmDelete() {
        val current = state.value
        val entryId = current.entryPendingDeletion ?: return
        val vehicle = current.vehicle ?: return
        if (current.deleting) return
        val uuid = runCatching { UUID.fromString(entryId) }.getOrNull()
        if (uuid == null) {
            mutableState.update {
                it.copy(
                    entryPendingDeletion = null,
                    result = EntrySaveResult.NotFound,
                    lastActionWasDelete = true,
                )
            }
            return
        }
        mutableState.update { it.copy(deleting = true) }
        viewModelScope.launch {
            val result = repository.deleteEntry(vehicle.id, uuid)
            mutableState.update {
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
        if (current.dashboard != null) {
            dashboardObservation?.cancel()
            mutableState.value = current.copy(dashboard = null)
            return
        }
        if (current.saving || current.openingEntry || current.deleting) return
        if (current.entryPendingDeletion != null) {
            cancelDelete()
            return
        }
        if (current.draft != null) {
            mutableState.value = current.copy(draft = null, result = null, fieldErrors = emptyMap(), lastActionWasDelete = false)
        } else {
            observation?.cancel()
            mutableState.value = LedgerUiState()
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
        }
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
        val remainingErrors = current.fieldErrors - fields
        mutableState.value = current.copy(
            draft = transform(draft),
            fieldErrors = if (clearsConflict) remainingErrors - EntryField.Mileage else remainingErrors,
            result = current.result.takeIf { it == EntrySaveResult.MileageConflict && !mileageChanged },
        )
    }

    fun saveWithoutMileage() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving || current.result != EntrySaveResult.MileageConflict) return
        mutableState.value = current.copy(draft = draft.copy(omitMileage = true))
        save()
    }

    fun save() {
        val current = state.value
        val draft = current.draft ?: return
        if (current.saving) return
        when (val validation = draft.validate()) {
            is DraftValidation.Invalid -> mutableState.value = current.copy(fieldErrors = validation.errors)
            is DraftValidation.Valid -> {
                mutableState.value = current.copy(saving = true, fieldErrors = emptyMap(), result = null, lastActionWasDelete = false)
                viewModelScope.launch {
                    val result = if (draft.editing) {
                        repository.update(draft.id, validation.input, validation.readingEdit)
                    } else {
                        repository.create(draft.id, draft.createdAt, validation.input, validation.kilometers)
                    }
                    mutableState.update {
                        it.copy(
                            draft = it.draft.takeUnless { result == EntrySaveResult.Saved },
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
}

private fun EntrySaveResult.fieldErrors(): Map<EntryField, Int> = when (this) {
    EntrySaveResult.InvalidOccurrenceDate -> mapOf(EntryField.Date to R.string.ledger_date_invalid)
    EntrySaveResult.InvalidAmount -> mapOf(EntryField.Amount to R.string.ledger_amount_invalid)
    EntrySaveResult.InvalidFuelVolume -> mapOf(EntryField.Volume to R.string.ledger_volume_invalid)
    EntrySaveResult.InvalidKilometers -> mapOf(EntryField.Mileage to R.string.ledger_mileage_invalid)
    EntrySaveResult.MileageConflict -> mapOf(EntryField.Mileage to R.string.ledger_mileage_conflict)
    else -> emptyMap()
}
