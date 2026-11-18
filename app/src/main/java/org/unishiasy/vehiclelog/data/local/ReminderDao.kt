package org.unishiasy.vehiclelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ReminderDao {
    @Insert
    suspend fun insert(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE vehicleId = :vehicleId ORDER BY id")
    fun observeForVehicle(vehicleId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE state = 'active' AND dueOn IS NOT NULL ORDER BY dueOn, id")
    suspend fun activeDateReminders(): List<ReminderEntity>

    @Query("UPDATE reminders SET state = :state WHERE vehicleId = :vehicleId AND id = :id AND state = 'active'")
    suspend fun finish(vehicleId: String, id: String, state: String): Int

    @Query("UPDATE reminders SET title = :title, dueOn = :dueOn, dueKilometers = :dueKilometers WHERE vehicleId = :vehicleId AND id = :id AND state = 'active'")
    suspend fun updateActive(
        vehicleId: String,
        id: String,
        title: String,
        dueOn: String?,
        dueKilometers: Long?,
    ): Int
}
