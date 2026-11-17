package org.unishiasy.vehiclelog.ui.vehicle

import android.database.sqlite.SQLiteException
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
    val draft: VehicleDraft? = null,
    val saving: Boolean = false,
    val saveResult: VehicleSaveResult? = null,
    val catalog: VehicleCatalog? = null,
)

internal data class VehicleDraft(val id: String? = null, val input: VehicleInput = VehicleInput(""))

internal class VehicleViewModel(
    private val repository: VehicleRepository,
    loadCatalog: suspend () -> VehicleCatalog? = { null },
) : ViewModel() {
    private val mutableState = MutableStateFlow(VehicleUiState())
    val state = mutableState.asStateFlow()

    init {
        observeVehicles()
        viewModelScope.launch {
            val catalog = loadCatalog()?.takeUnless { it.isEmpty }
            mutableState.update { it.copy(catalog = catalog) }
        }
    }

    fun retryList() {
        if (!mutableState.value.listFailed) return
        mutableState.update { it.copy(listFailed = false) }
        observeVehicles()
    }

    private fun observeVehicles() {
        viewModelScope.launch {
            repository.observeAll()
                .catch { failure ->
                    when (failure) {
                        is CancellationException -> throw failure
                        is SQLiteException, is IllegalStateException ->
                            mutableState.update { it.copy(listFailed = true) }
                        else -> throw failure
                    }
                }
                .collect { vehicles -> mutableState.update { it.copy(vehicles = vehicles) } }
        }
    }

    fun addVehicle() {
        mutableState.update { it.copy(draft = VehicleDraft(), saveResult = null) }
    }

    fun editVehicle(vehicle: VehicleEntity) {
        val input = VehicleInput(
            displayLabel = vehicle.displayLabel,
            catalogKey = vehicle.catalogKey,
            productionYear = vehicle.productionYear?.toPersianYear(),
            productionYearCalendar = vehicle.productionYearCalendar,
        )
        mutableState.update { it.copy(draft = VehicleDraft(vehicle.id, input), saveResult = null) }
    }

    fun changeInput(change: (VehicleInput) -> VehicleInput) {
        mutableState.update { current ->
            val draft = current.draft
            if (draft == null || current.saving) current
            else current.copy(draft = draft.copy(input = change(draft.input)), saveResult = null)
        }
    }

    fun selectCatalogEntry(entry: VehicleCatalogEntry) {
        mutableState.update { current ->
            val draft = current.draft
            if (draft == null || current.saving || current.catalog == null) return@update current

            val input = draft.input.copy(displayLabel = entry.displayLabel, catalogKey = entry.key)
            val result = current.saveResult.takeUnless {
                it == VehicleSaveResult.InvalidInput(VehicleInputError.BlankDisplayLabel)
            }
            current.copy(draft = draft.copy(input = input), saveResult = result)
        }
    }

    fun cancel() {
        mutableState.update { if (it.saving) it else it.copy(draft = null, saveResult = null) }
    }

    fun save() {
        val current = mutableState.value
        val draft = current.draft ?: return
        if (current.saving) return
        mutableState.update { it.copy(saving = true, saveResult = null) }
        viewModelScope.launch {
            val result = if (draft.id == null) repository.create(draft.input)
            else repository.update(draft.id, draft.input)
            val saved = result is VehicleSaveResult.Created || result is VehicleSaveResult.Updated
            mutableState.update {
                it.copy(draft = if (saved) null else draft, saving = false, saveResult = result)
            }
        }
    }
}

internal fun Int.toPersianYear(): String =
    toString().padStart(4, '0').map { '۰' + (it - '0') }.joinToString("")
