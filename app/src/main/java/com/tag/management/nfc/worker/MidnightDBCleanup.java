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

    public MidnightDBCleanup(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        String employerUid = getInputData().getString("employer_uid");

        //avoid multiple jobs which seems a bug in the SDK
        if (TimesheetUtil.isDoing) {
            return Result.failure();
        } else {
            TimesheetUtil.isDoing = true;

            String fBDbName = TimesheetUtil.getCurrentDateUsingCalendar();
            fBDbName = fBDbName.replace(".", "-").replace(" ", "-").replace("#", "-").replace("$", "-").replace("[", "-").replace("]", "-");
            FirebaseDatabase mFirebaseDatabase = FirebaseDatabase.getInstance();

            mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(fBDbName).child("" + (employerUid.isEmpty() ? TimesheetUtil.getEmployerUid(this.mContext) : employerUid));

            // midnight DB clean up
            Log.d("worker", " DB cleaning worker is running");

            //check if around one hour from midnight
            if (TimesheetUtil.getAbsoluteMillisTillMidnight() > 3600000) {
                //run once off workers
                OneTimeWorkRequest midnightWorkRequest =
                        new OneTimeWorkRequest.Builder(MidnightFinder.class)
                                .setInitialDelay(TimesheetUtil.getMillisTillMidnight(), TimeUnit.MILLISECONDS)
                                .build();
                TimesheetUtil.isDoing = false;
                WorkManager.getInstance().enqueue(midnightWorkRequest);
                return Result.failure();

            } else {
                dailyActivityDb = DailyActivityDatabase.getInstance(getApplicationContext());
                employeeListDb = AppDatabase.getInstance(getApplicationContext());

                List<DailyActivityEntry> staff = dailyActivityDb.dailyActivityDao().loadAllEmployees();
                for (DailyActivityEntry entry : staff) {
                    int inCounter = entry.getEmployeeTimestampIn().split("-").length;
                    int outCounter = entry.getEmployeeTimestampOut().split("-").length;

                    if (inCounter == outCounter) {
                        // ok to send to fb

                    } else if (inCounter > outCounter) {
                        // add midnight to db
                        DailyActivityEntry employee;
                        employee = entry;
                        employee.setEmployeeTimestampOut(employee.getEmployeeTimestampOut() + "-" + TimesheetUtil.getCurrentTimeUsingCalendar());
                        dailyActivityDb.dailyActivityDao().updateEmployee(employee);

                    } else {
                        Log.e("MidnightDBCleanup", "Wrong info in daily activity DB");
                    }
                }
                uploadStaffFB(staff);
                return Result.success();
            }
        }
    }

    private void uploadStaffFB(List<DailyActivityEntry> staff) {
        for (DailyActivityEntry entry : staff) {
            mMessagesDatabaseReference.push().setValue(entry);
            updateStaffAvailability(entry.getEmployeeUniqueId(), false);
            dailyActivityDb.dailyActivityDao().deleteEmployee(entry);
        }

        TimesheetUtil.isDoing = false;
    }

    private void updateStaffAvailability(String uId, boolean availability) {
        if (employeeListDb.employeeDao().loadEmployeeByUid(uId) != null) {
            EmployeeEntry employee;
            employee = employeeListDb.employeeDao().loadEmployeeByUid(uId);
            employee.setEmployeeAvailable(availability);
            employeeListDb.employeeDao().updateEmployee(employee);
        }
    }

}