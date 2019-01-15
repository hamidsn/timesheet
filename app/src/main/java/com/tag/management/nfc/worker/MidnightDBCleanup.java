package com.tag.management.nfc.worker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.tag.management.nfc.TimesheetUtil;
import com.tag.management.nfc.database.DailyActivityDatabase;
import com.tag.management.nfc.database.DailyActivityEntry;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MidnightDBCleanup extends Worker {

    private final Context mContext;
    private DailyActivityDatabase dailyActivityDb;
    private List<DailyActivityEntry> staff;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private static final String TIMES = "times";

    public MidnightDBCleanup(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {

        //avoid multiple jobs which seems a bug in the SDK
        if (TimesheetUtil.isDoing) {
            return Result.failure();
        } else {
            TimesheetUtil.isDoing = true;

            String fBDbName = TimesheetUtil.getCurrentDateUsingCalendar();
            fBDbName = fBDbName.replace(".", "-").replace(" ", "-").replace("#", "-").replace("$", "-").replace("[", "-").replace("]", "-");
            mFirebaseDatabase = FirebaseDatabase.getInstance();
            mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(fBDbName).child("-"+ TimesheetUtil.getEmployerUid());

            // midnight DB clean up
            Log.d("worker", " DB cleaning worker is running");

            //check if around one night from midnight
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
                staff = dailyActivityDb.dailyActivityDao().loadAllEmployees();
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
        for (DailyActivityEntry entry : staff){
            mMessagesDatabaseReference.push().setValue(entry);
             dailyActivityDb.dailyActivityDao().deleteEmployee(entry);
        }

        TimesheetUtil.isDoing = false;
    }

}