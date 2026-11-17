package org.unishiasy.vehiclelog.data.local

import androidx.room.Dao
import androidx.room.Query

@Dao
internal interface BackupDao {
    @Query("""
        SELECT NOT EXISTS (SELECT 1 FROM vehicles)
           AND NOT EXISTS (SELECT 1 FROM entries)
           AND NOT EXISTS (SELECT 1 FROM odometer_readings)
           AND NOT EXISTS (SELECT 1 FROM reminders)
    """)
    suspend fun isEmpty(): Boolean

    @Query("SELECT * FROM vehicles LIMIT :limit")
    suspend fun vehicles(limit: Int): List<VehicleEntity>

    @Query("SELECT * FROM entries LIMIT :limit")
    suspend fun entries(limit: Int): List<EntryEntity>

    @Query("SELECT * FROM odometer_readings LIMIT :limit")
    suspend fun readings(limit: Int): List<OdometerReadingEntity>

    @Query("SELECT * FROM reminders LIMIT :limit")
    suspend fun reminders(limit: Int): List<ReminderEntity>
}
