package org.unishiasy.vehiclelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface LedgerDao {
    @Query("""
        SELECT
            entries.id,
            entries.vehicleId,
            entries.category,
            entries.occurredOn,
            entries.createdAtEpochMillis,
            entries.notes,
            entries.amountRials,
            entries.fuelVolumeMl,
            entries.station_name AS stationName,
            entries.is_full_tank AS isFullTank,
            odometer_readings.kilometers AS linkedOdometerKm
        FROM entries
        LEFT JOIN odometer_readings ON odometer_readings.entryId = entries.id
        WHERE entries.vehicleId = :vehicleId
        ORDER BY entries.occurredOn DESC, entries.createdAtEpochMillis DESC, entries.id ASC
    """)
    fun observeHistoryForVehicle(vehicleId: String): Flow<List<HistoryEntry>>

    @Query("""
        SELECT * FROM entries WHERE vehicleId = :vehicleId
        ORDER BY occurredOn DESC, createdAtEpochMillis DESC, id ASC
    """)
    fun observeForVehicle(vehicleId: String): Flow<List<EntryEntity>>

    @Query("""
        SELECT * FROM entries WHERE vehicleId = :vehicleId
        ORDER BY occurredOn DESC, createdAtEpochMillis DESC, id ASC LIMIT 5
    """)
    fun observeRecentForVehicle(vehicleId: String): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun findById(id: String): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity)

    @Query("""
        UPDATE entries SET vehicleId = :vehicleId, category = :category, occurredOn = :occurredOn,
            notes = :notes, amountRials = :amountRials, fuelVolumeMl = :fuelVolumeMl,
            station_name = :stationName, is_full_tank = :isFullTank
        WHERE id = :id
    """)
    suspend fun update(
        id: String,
        vehicleId: String,
        category: String,
        occurredOn: String,
        notes: String?,
        amountRials: Long?,
        fuelVolumeMl: Long?,
        stationName: String? = null,
        isFullTank: Boolean = false,
    ): Int

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("DELETE FROM entries WHERE id = :id AND vehicleId = :vehicleId")
    suspend fun deleteForVehicle(id: String, vehicleId: String): Int
}
