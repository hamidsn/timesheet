package com.tag.management.nfc.database;

import android.content.Context;
import android.util.Log;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ReportEntry.class}, version = 5, exportSchema = false)
public abstract class ReportDatabase extends RoomDatabase {

    private static final String LOG_TAG = ReportDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "timesheetReportNFC";
    private static ReportDatabase sInstance;

    public static ReportDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance for report");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                        ReportDatabase.class, ReportDatabase.DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .build();
            }
        }
        Log.d(LOG_TAG, "Getting the database instance");
        return sInstance;
    }

    public abstract ReportDao reportDao();

}
