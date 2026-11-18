package org.unishiasy.vehiclelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface OdometerDao {
    @Query("SELECT * FROM odometer_readings WHERE id = :id")
    suspend fun findById(id: String): OdometerReadingEntity?

    @Query("SELECT * FROM odometer_readings WHERE entryId = :entryId")
    suspend fun findForEntry(entryId: String): OdometerReadingEntity?

    @Query("""
        SELECT kilometers FROM odometer_readings WHERE vehicleId = :vehicleId
        ORDER BY occurredOn DESC, kilometers DESC LIMIT 1
    """)
    fun observeCurrentMileage(vehicleId: String): Flow<Long?>

    @Query("""
        SELECT * FROM odometer_readings
        WHERE vehicleId = :vehicleId AND entryId IS NULL
        ORDER BY occurredOn DESC, kilometers DESC, id
    """)
    fun observeStandaloneForVehicle(vehicleId: String): Flow<List<OdometerReadingEntity>>

    @Query("""
        SELECT MAX(kilometers) FROM odometer_readings
        WHERE vehicleId = :vehicleId AND occurredOn < :day AND id != :excludedId
    """)
    suspend fun earlierMaximum(vehicleId: String, day: String, excludedId: String): Long?

    @Query("""
        SELECT MIN(kilometers) FROM odometer_readings
        WHERE vehicleId = :vehicleId AND occurredOn > :day AND id != :excludedId
    """)
    suspend fun laterMinimum(vehicleId: String, day: String, excludedId: String): Long?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reading: OdometerReadingEntity)

    @Update(onConflict = OnConflictStrategy.ABORT)
    suspend fun update(reading: OdometerReadingEntity)

    @Query("DELETE FROM odometer_readings WHERE entryId = :entryId")
    suspend fun deleteForEntry(entryId: String)

    @Query("DELETE FROM odometer_readings WHERE id = :id AND entryId IS NULL")
    suspend fun deleteStandalone(id: String): Int
}
