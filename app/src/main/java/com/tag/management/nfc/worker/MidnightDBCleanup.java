package com.tag.management.nfc.worker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tag.management.nfc.TimesheetUtil;
import com.tag.management.nfc.database.AppDatabase;
import com.tag.management.nfc.database.DailyActivityDatabase;
import com.tag.management.nfc.database.DailyActivityEntry;
import com.tag.management.nfc.database.EmployeeEntry;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MidnightDBCleanup extends Worker {

    // private static final String TIMES = "times";
    private final Context mContext;
    private DailyActivityDatabase dailyActivityDb;
    private AppDatabase employeeListDb;
    private DatabaseReference mMessagesDatabaseReference;
    private static final String DASH_CHAR = "-";
    private static final String EMPLOYER_UID_INFO = "employer_uid_info";
    private static final String EMPTY_EMPLOYER_UID = "EMPTY_EMPLOYER_UID";

    public MidnightDBCleanup(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        Log.d("worker:", " DB cleaning worker is started");
        String employerUid = getInputData().getString(EMPLOYER_UID_INFO);
        if(employerUid == null || employerUid.isEmpty()){
            employerUid = TimesheetUtil.getEmployerUid(this.mContext);
        }
        if(employerUid.isEmpty()){
            employerUid = EMPTY_EMPLOYER_UID;
        }
        Log.d("worker:", " employerUid = " + employerUid);

        //avoid multiple jobs which seems a bug in the SDK
        if (TimesheetUtil.isDoing) {
            return Result.failure();
        } else {
            TimesheetUtil.isDoing = true;
            FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();

            String fBDbMonth = TimesheetUtil.getCurrentMonthUsingCalendar();
            fBDbMonth = TimesheetUtil.validateStringFB(fBDbMonth);

            String fBDbDay = TimesheetUtil.getCurrentDayUsingCalendar();
            fBDbDay = TimesheetUtil.validateStringFB(fBDbDay);

            String fBDbYear = TimesheetUtil.getCurrentYearUsingCalendar();
            fBDbYear = TimesheetUtil.validateStringFB(fBDbYear);

            mMessagesDatabaseReference = mFirebaseDatabase.getReference()
                    .child(fBDbYear)
                    .child(fBDbMonth)
                    .child(fBDbDay)
                    .child(employerUid);

            // midnight DB clean up
            Log.d("worker:", " DB cleaning worker is running");

            //check if around one hour from midnight
            if (TimesheetUtil.getAbsoluteMillisTillMidnight() > 3600000) {
                Log.d("worker", " DB getAbsoluteMillisTillMidnight greater than one hour");
                //run once off workers
                    //WorkManager.getInstance().cancelAllWorkByTag(TimesheetUtil.WORKERTAG);
                Log.d("worker", " Previous DB cleaning worker is NOT running - Retrying in 58 mins");

                    //TimesheetUtil.applyOnceoffWorker();
                /*WorkManager workerInstance = WorkManager.getInstance();
                OneTimeWorkRequest midnightWorkRequest =
                        new OneTimeWorkRequest.Builder(MidnightFinder.class)
                                .setInitialDelay(TimesheetUtil.getMinutesTillMidnight(), TimeUnit.MINUTES)
                                .build();
                TimesheetUtil.isDoing = false;
                try {
                    workerInstance.enqueueUniqueWork("HAMID", ExistingWorkPolicy.REPLACE, midnightWorkRequest);
                } catch (Exception e) {
                    Log.d("worker", "error" + e.getMessage());
                }*/
                TimesheetUtil.isDoing = false;
                return Result.retry();

            } else {
                dailyActivityDb = DailyActivityDatabase.getInstance(getApplicationContext());
                employeeListDb = AppDatabase.getInstance(getApplicationContext());
                Log.d("worker:", " DB getting all staff");

                List<DailyActivityEntry> staff = dailyActivityDb.dailyActivityDao().loadAllEmployees();
                for (DailyActivityEntry entry : staff) {
                    int inCounter = entry.getEmployeeTimestampIn().split(DASH_CHAR).length;
                    int outCounter = entry.getEmployeeTimestampOut().split(DASH_CHAR).length;

                    Log.d("worker:", " DB correcting outCounter");

                    if (inCounter == outCounter) {
                        // ok to send to fb

                    } else if (inCounter > outCounter) {
                        // add midnight to db
                        Log.d("worker:", " DB add midnight to db");
                        DailyActivityEntry employee;
                        employee = entry;
                        employee.setEmployeeTimestampOut(employee.getEmployeeTimestampOut() + DASH_CHAR + TimesheetUtil.getCurrentTimeUsingCalendar());
                        dailyActivityDb.dailyActivityDao().updateEmployee(employee);

                    } else {
                        Log.e("MidnightDBCleanup", "Wrong info in daily activity DB");
                    }
                }
                Log.d("worker:", " DB uploading to firebase");
                uploadStaffFB(staff);
                return Result.success();
            }
        }
    }

    private void uploadStaffFB(List<DailyActivityEntry> staff) {
        for (DailyActivityEntry entry : staff) {
            Log.d("worker:", " pushing db to fb");
            mMessagesDatabaseReference.push().setValue(entry);
            updateStaffAvailability(entry.getEmployeeUniqueId(), false);
            dailyActivityDb.dailyActivityDao().deleteEmployee(entry);
        }
        Log.d("worker:", " DB pushed to firebase");
        TimesheetUtil.isDoing = false;
    }

    private void updateStaffAvailability(String uId, boolean availability) {
        Log.d("worker:", " DB availability update");
        if (employeeListDb.employeeDao().loadEmployeeByUid(uId) != null) {
            EmployeeEntry employee;
            employee = employeeListDb.employeeDao().loadEmployeeByUid(uId);
            employee.setEmployeeAvailable(availability);
            employeeListDb.employeeDao().updateEmployee(employee);
        }

    }

}