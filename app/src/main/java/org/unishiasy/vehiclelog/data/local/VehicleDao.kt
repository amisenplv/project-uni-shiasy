package org.unishiasy.vehiclelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY id")
    fun observeAll(): Flow<List<VehicleEntity>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun findById(id: String): VehicleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vehicle: VehicleEntity)

    @Update
    suspend fun update(vehicle: VehicleEntity): Int
}
