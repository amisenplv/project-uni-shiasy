package org.unishiasy.vehiclelog.data

import android.database.sqlite.SQLiteException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import org.unishiasy.vehiclelog.data.local.VehicleDao
import org.unishiasy.vehiclelog.data.local.VehicleEntity

sealed interface VehicleSaveResult {
    data class Created(val id: String) : VehicleSaveResult
    data class Updated(val id: String) : VehicleSaveResult
    data class InvalidInput(val reason: VehicleInputError) : VehicleSaveResult
    data object NotFound : VehicleSaveResult
    data object StorageFailure : VehicleSaveResult
}

class VehicleRepository(private val dao: VehicleDao) {
    fun observeAll(): Flow<List<VehicleEntity>> = dao.observeAll()

    suspend fun findById(id: String): VehicleEntity? = dao.findById(id)

    suspend fun create(input: VehicleInput): VehicleSaveResult =
        save(UUID.randomUUID().toString(), input) { vehicle ->
            dao.insert(vehicle)
            VehicleSaveResult.Created(vehicle.id)
        }

    suspend fun update(id: String, input: VehicleInput): VehicleSaveResult =
        save(id, input) { vehicle ->
            if (dao.update(vehicle) == 0) {
                VehicleSaveResult.NotFound
            } else {
                VehicleSaveResult.Updated(vehicle.id)
            }
        }

    private suspend fun save(
        id: String,
        input: VehicleInput,
        write: suspend (VehicleEntity) -> VehicleSaveResult,
    ): VehicleSaveResult = when (val validation = validateVehicleInput(id, input)) {
        is VehicleInputValidation.Invalid -> VehicleSaveResult.InvalidInput(validation.reason)
        is VehicleInputValidation.Valid -> try {
            write(validation.vehicle)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SQLiteException) {
            VehicleSaveResult.StorageFailure
        } catch (_: IllegalStateException) {
            VehicleSaveResult.StorageFailure
        }
    }
}
