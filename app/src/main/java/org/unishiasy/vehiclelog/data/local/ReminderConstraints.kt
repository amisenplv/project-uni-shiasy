package org.unishiasy.vehiclelog.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

// Room does not declare CHECK clauses. Install the same guards on both database creation paths.
internal val reminderConstraints = object : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        installReminderConstraints(db)
    }
}

internal fun installReminderConstraints(db: SupportSQLiteDatabase) {
    for (operation in listOf("INSERT", "UPDATE")) {
        db.execSQL(
            """
            CREATE TRIGGER reminder_validate_${operation.lowercase()}
            BEFORE $operation ON reminders
            WHEN (NEW.dueOn IS NULL) = (NEW.dueKilometers IS NULL)
              OR (NEW.dueKilometers IS NOT NULL AND (typeof(NEW.dueKilometers) != 'integer' OR NEW.dueKilometers < 0))
              OR NEW.state NOT IN ('active', 'completed', 'canceled')
              OR trim(NEW.title) = ''
              OR (NEW.dueOn IS NOT NULL AND (
                  NEW.dueOn NOT GLOB '[0-9][0-9][0-9][0-9]-[0-9][0-9]-[0-9][0-9]'
                  OR substr(NEW.dueOn, 1, 4) = '0000'
                  OR date(NEW.dueOn, '+0 days') IS NOT NEW.dueOn
              ))
            BEGIN
                SELECT RAISE(ABORT, 'Invalid reminder');
            END
            """.trimIndent(),
        )
    }
}
