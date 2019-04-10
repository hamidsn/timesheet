package com.tag.management.nfc.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.util.Log;

import com.tag.management.nfc.TimesheetUtil;
import com.tag.management.nfc.worker.MidnightFinder;

import java.util.concurrent.TimeUnit;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

@Database(entities = {DailyActivityEntry.class}, version = 13, exportSchema = false)
public abstract class DailyActivityDatabase extends RoomDatabase {

    private static final String LOG_TAG = DailyActivityDatabase.class.getSimpleName();
    private static final Object LOCK = new Object();
    private static final String DATABASE_NAME = "dailyAvticityNFC";
    private static DailyActivityDatabase sInstance;

    public static DailyActivityDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (LOCK) {
                Log.d(LOG_TAG, "Creating new database instance");
                sInstance = Room.databaseBuilder(context.getApplicationContext(),
                        DailyActivityDatabase.class, DailyActivityDatabase.DATABASE_NAME)
                        .fallbackToDestructiveMigration()
                        .build();
            }

            TimesheetUtil.applyOnceoffWorker();
            /*WorkManager workerInstance = WorkManager.getInstance();
            //run once off workers
            OneTimeWorkRequest midnightWorkRequest =
                    new OneTimeWorkRequest.Builder(MidnightFinder.class)
                            .setInitialDelay(TimesheetUtil.getMinutesTillMidnight(), TimeUnit.MINUTES)
                            //.setInitialDelay(17L, TimeUnit.MINUTES)
                            .build();
            Log.d("worker", "running midnight finder with " + TimesheetUtil.getMinutesTillMidnight() + " Minutes");
            try {
                workerInstance.enqueueUniqueWork("HAMID", ExistingWorkPolicy.REPLACE, midnightWorkRequest);
            } catch (Exception e) {
                Log.d("worker", "error" + e.getMessage());
            }*/
        }

        Log.d(LOG_TAG, "Getting the database instance");
        return sInstance;
    }

    public abstract DailyActivityDao dailyActivityDao();

}
