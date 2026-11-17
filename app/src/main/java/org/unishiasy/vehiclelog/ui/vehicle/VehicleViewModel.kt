package org.unishiasy.vehiclelog.ui.vehicle

import android.database.sqlite.SQLiteException
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.data.VehicleInput
import org.unishiasy.vehiclelog.data.VehicleInputError
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.VehicleSaveResult
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalog
import org.unishiasy.vehiclelog.data.catalog.VehicleCatalogEntry
import org.unishiasy.vehiclelog.data.local.VehicleEntity

internal data class VehicleUiState(
    val vehicles: List<VehicleEntity>? = null,
    val listFailed: Boolean = false,
    val managingList: Boolean = false,
    val draft: VehicleDraft? = null,
    val saving: Boolean = false,
    val saveResult: VehicleSaveResult? = null,
    val catalog: VehicleCatalog? = null,
)

internal data class VehicleDraft(val id: String? = null, val input: VehicleInput = VehicleInput(""))

internal class VehicleViewModel(
    private val repository: VehicleRepository,
    private val savedStateHandle: SavedStateHandle? = null,
    loadCatalog: suspend () -> VehicleCatalog? = { null },
) : ViewModel() {
    private val mutableState = MutableStateFlow(restoreInitialState(savedStateHandle))
    val state = mutableState.asStateFlow()

    init {
        observeVehicles()
        viewModelScope.launch {
            val catalog = loadCatalog()?.takeUnless { it.isEmpty }
            updateState { it.copy(catalog = catalog) }
        }
    }

    fun retryList() {
        if (!mutableState.value.listFailed) return
        updateState { it.copy(listFailed = false) }
        observeVehicles()
    }

    private fun observeVehicles() {
        viewModelScope.launch {
            repository.observeAll()
                .catch { failure ->
                    when (failure) {
                        is CancellationException -> throw failure
                        is SQLiteException, is IllegalStateException ->
                            updateState { it.copy(listFailed = true) }
                        else -> throw failure
                    }
                }
                .collect { vehicles -> updateState { it.copy(vehicles = vehicles) } }
        }
    }

    fun openManagement() {
        updateState { it.copy(managingList = true, saveResult = null) }
    }

    fun closeManagement() {
        updateState { if (it.saving) it else it.copy(managingList = false, draft = null, saveResult = null) }
    }

    fun clearSaveResult() {
        updateState { if (it.draft == null) it.copy(saveResult = null) else it }
    }

    fun addVehicle() {
        updateState { it.copy(draft = VehicleDraft(), saveResult = null) }
    }

    fun editVehicle(vehicle: VehicleEntity) {
        val input = VehicleInput(
            displayLabel = vehicle.displayLabel,
            catalogKey = vehicle.catalogKey,
            productionYear = vehicle.productionYear?.toPersianYear(),
            productionYearCalendar = vehicle.productionYearCalendar,
        )
        updateState { it.copy(draft = VehicleDraft(vehicle.id, input), saveResult = null) }
    }

    fun changeInput(change: (VehicleInput) -> VehicleInput) {
        updateState { current ->
            val draft = current.draft
            if (draft == null || current.saving) current
            else current.copy(draft = draft.copy(input = change(draft.input)), saveResult = null)
        }
    }

    fun selectCatalogEntry(entry: VehicleCatalogEntry) {
        updateState { current ->
            val draft = current.draft
            if (draft == null || current.saving || current.catalog == null) return@updateState current

            val input = draft.input.copy(displayLabel = entry.displayLabel, catalogKey = entry.key)
            val result = current.saveResult.takeUnless {
                it == VehicleSaveResult.InvalidInput(VehicleInputError.BlankDisplayLabel)
            }
            current.copy(draft = draft.copy(input = input), saveResult = result)
        }
    }

    fun cancel() {
        updateState { if (it.saving) it else it.copy(draft = null, saveResult = null) }
    }

    fun save() {
        val current = mutableState.value
        val draft = current.draft ?: return
        if (current.saving) return
        updateState { it.copy(saving = true, saveResult = null) }
        viewModelScope.launch {
            val result = if (draft.id == null) repository.create(draft.input)
            else repository.update(draft.id, draft.input)
            val saved = result is VehicleSaveResult.Created || result is VehicleSaveResult.Updated
            updateState {
                it.copy(draft = if (saved) null else draft, saving = false, saveResult = result)
            }
        }
    }

    private inline fun updateState(transform: (VehicleUiState) -> VehicleUiState) {
        var nextState: VehicleUiState? = null
        mutableState.update { current ->
            transform(current).also { nextState = it }
        }
        nextState?.let(::persistState)
    }

    private fun persistState(state: VehicleUiState) {
        val handle = savedStateHandle ?: return
        handle[KEY_MANAGING] = state.managingList
        val draft = state.draft
        handle[KEY_DRAFT_PRESENT] = draft != null
        handle[KEY_DRAFT_ID] = draft?.id
        handle[KEY_DRAFT_LABEL] = draft?.input?.displayLabel
        handle[KEY_DRAFT_CATALOG_KEY] = draft?.input?.catalogKey
        handle[KEY_DRAFT_YEAR] = draft?.input?.productionYear
        handle[KEY_DRAFT_CALENDAR] = draft?.input?.productionYearCalendar
    }

    companion object {
        private const val KEY_MANAGING = "vehicle_vm_managing"
        private const val KEY_DRAFT_PRESENT = "vehicle_vm_draft_present"
        private const val KEY_DRAFT_ID = "vehicle_vm_draft_id"
        private const val KEY_DRAFT_LABEL = "vehicle_vm_draft_label"
        private const val KEY_DRAFT_CATALOG_KEY = "vehicle_vm_draft_catalog_key"
        private const val KEY_DRAFT_YEAR = "vehicle_vm_draft_year"
        private const val KEY_DRAFT_CALENDAR = "vehicle_vm_draft_calendar"

        private fun restoreInitialState(handle: SavedStateHandle?): VehicleUiState {
            if (handle == null) return VehicleUiState()
            val managing = handle.get<Boolean>(KEY_MANAGING) == true
            val draft = if (handle.get<Boolean>(KEY_DRAFT_PRESENT) == true) {
                VehicleDraft(
                    id = handle.get<String>(KEY_DRAFT_ID),
                    input = VehicleInput(
                        displayLabel = handle.get<String>(KEY_DRAFT_LABEL).orEmpty(),
                        catalogKey = handle.get<String>(KEY_DRAFT_CATALOG_KEY),
                        productionYear = handle.get<String>(KEY_DRAFT_YEAR),
                        productionYearCalendar = handle.get<String>(KEY_DRAFT_CALENDAR),
                    ),
                )
            } else null
            return VehicleUiState(managingList = managing, draft = draft)
        }
    }
}

internal fun Int.toPersianYear(): String =
    toString().padStart(4, '0').map { '۰' + (it - '0') }.joinToString("")
