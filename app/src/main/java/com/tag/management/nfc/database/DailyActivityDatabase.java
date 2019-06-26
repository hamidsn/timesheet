package com.tag.management.nfc.database;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import android.util.Log;

import com.tag.management.nfc.TimesheetUtil;

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
            //TimesheetUtil.applyDailyWorker(context);
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
