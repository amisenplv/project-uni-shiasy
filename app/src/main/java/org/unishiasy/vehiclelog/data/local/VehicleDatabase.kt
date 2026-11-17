package org.unishiasy.vehiclelog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [VehicleEntity::class, EntryEntity::class, OdometerReadingEntity::class, ReminderEntity::class],
    version = 4,
    exportSchema = true,
)
abstract class VehicleDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    internal abstract fun ledgerDao(): LedgerDao
    internal abstract fun odometerDao(): OdometerDao
    internal abstract fun reminderDao(): ReminderDao
    internal abstract fun backupDao(): BackupDao
}
