package org.unishiasy.vehiclelog.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `entries` (
                `id` TEXT NOT NULL,
                `vehicleId` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `occurredOn` TEXT NOT NULL,
                `createdAtEpochMillis` INTEGER NOT NULL,
                `notes` TEXT,
                `amountRials` INTEGER,
                `fuelVolumeMl` INTEGER,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_entries_vehicleId_occurredOn` ON `entries` (`vehicleId`, `occurredOn`)",
        )
    }
}

internal val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `odometer_readings` (
                `id` TEXT NOT NULL,
                `vehicleId` TEXT NOT NULL,
                `occurredOn` TEXT NOT NULL,
                `kilometers` INTEGER NOT NULL,
                `entryId` TEXT,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                FOREIGN KEY(`entryId`) REFERENCES `entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_odometer_readings_vehicleId_occurredOn` " +
                "ON `odometer_readings` (`vehicleId`, `occurredOn`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_odometer_readings_entryId` ON `odometer_readings` (`entryId`)",
        )
    }
}

internal val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminders` (
                `id` TEXT NOT NULL,
                `vehicleId` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `dueOn` TEXT,
                `dueKilometers` INTEGER,
                `state` TEXT NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`vehicleId`) REFERENCES `vehicles`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminders_vehicleId` ON `reminders` (`vehicleId`)")
        installReminderConstraints(db)
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `entries` ADD COLUMN `station_name` TEXT")
        db.execSQL("ALTER TABLE `entries` ADD COLUMN `is_full_tank` INTEGER NOT NULL DEFAULT 0")
    }
}

