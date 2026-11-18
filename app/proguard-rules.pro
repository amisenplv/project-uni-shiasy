# Keep entry points and Android framework subclasses intact under R8.
-keep class org.unishiasy.vehiclelog.VehicleLedgerApplication { *; }
-keep class org.unishiasy.vehiclelog.MainActivity { *; }
-keep class org.unishiasy.vehiclelog.reminders.ReminderWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Room generated implementations and migrations.
-keep class * extends androidx.room.RoomDatabase
-keep class org.unishiasy.vehiclelog.data.local.** { *; }
-dontwarn androidx.room.paging.**
