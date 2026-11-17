package org.unishiasy.vehiclelog.ui.home

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.unishiasy.vehiclelog.data.LedgerRepository
import org.unishiasy.vehiclelog.data.VehicleRepository
import org.unishiasy.vehiclelog.data.local.VehicleEntity

data class HomeUiState(
    val vehicles: List<VehicleEntity>? = null,
    val selectedVehicleId: String? = null,
    val selectedVehicleMileage: Long? = null,
    val mileageLoaded: Boolean = false,
    val listFailed: Boolean = false,
    val recentHistory: List<org.unishiasy.vehiclelog.data.local.HistoryEntry>? = null,
) {
    val selectedVehicle: VehicleEntity?
        get() = vehicles?.firstOrNull { it.id == selectedVehicleId }
            ?: vehicles?.firstOrNull()
}

class HomeViewModel(
    private val vehicleRepository: VehicleRepository,
    private val ledgerRepository: LedgerRepository,
    coroutineScope: CoroutineScope? = null,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState())
    val state = mutableState.asStateFlow()

    private val scope: CoroutineScope = coroutineScope ?: viewModelScope
    private var vehiclesJob: Job? = null
    private var mileageJob: Job? = null

    init {
        observeVehicles()
    }

    fun selectVehicle(vehicleId: String) {
        if (state.value.selectedVehicleId == vehicleId) return
        mutableState.update {
            it.copy(
                selectedVehicleId = vehicleId,
                selectedVehicleMileage = null,
                mileageLoaded = false,
            )
        }
        observeMileageForSelected()
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
                    mutableState.update { current ->
                        val selectedId = current.selectedVehicleId?.takeIf { id -> list.any { it.id == id } }
                            ?: list.firstOrNull()?.id
                        current.copy(
                            vehicles = list,
                            selectedVehicleId = selectedId,
                            listFailed = false,
                        )
                    }
                    observeMileageForSelected()
                }
        }
    }

    private fun observeMileageForSelected() {
        val selectedId = state.value.selectedVehicle?.id
        mileageJob?.cancel()
        if (selectedId == null) {
            mutableState.update { it.copy(selectedVehicleMileage = null, mileageLoaded = true, recentHistory = emptyList()) }
            return
        }
        mileageJob = scope.launch {
            launch {
                ledgerRepository.observeCurrentMileage(selectedId)
                    .catch { failure ->
                        if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                        mutableState.update { it.copy(selectedVehicleMileage = null, mileageLoaded = true) }
                    }
                    .collect { mileage ->
                        mutableState.update { it.copy(selectedVehicleMileage = mileage, mileageLoaded = true) }
                    }
            }
            launch {
                ledgerRepository.observeHistoryForVehicle(selectedId)
                    .catch { failure ->
                        if (failure !is SQLiteException && failure !is IllegalStateException) throw failure
                        mutableState.update { it.copy(recentHistory = emptyList()) }
                    }
                    .collect { history ->
                        mutableState.update { it.copy(recentHistory = history.take(2)) }
                    }
            }
        }
    }
}
