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

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MidnightDBCleanup extends Worker {

    private static final String DASH_CHAR = "-";
    private static final String EMPLOYER_UID_INFO = "employer_uid_info";
    private static final String EMPTY_EMPLOYER_UID = "EMPTY_EMPLOYER_UID";
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

        Log.d("worker:", " DB cleaning worker is started");
        String employerUid = getInputData().getString(EMPLOYER_UID_INFO);
        if (employerUid == null || employerUid.isEmpty()) {
            employerUid = TimesheetUtil.getEmployerUid(this.mContext);
        }
        if (employerUid.isEmpty()) {
            employerUid = EMPTY_EMPLOYER_UID;
        }
        Log.d("worker:", " employerUid = " + employerUid);

        //avoid multiple jobs which seems a bug in the SDK
        if (TimesheetUtil.isDoing) {
            Log.d("worker:", " isDoing  is true");
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
            {
                dailyActivityDb = DailyActivityDatabase.getInstance(getApplicationContext());
                employeeListDb = AppDatabase.getInstance(getApplicationContext());
                Log.d("worker:", " DB getting all staff");

                List<DailyActivityEntry> staff = dailyActivityDb.dailyActivityDao().loadAllEmployees();

                Log.d("worker:", " DB uploading to firebase");
                uploadStaffFB(staff);
                TimesheetUtil.isDoing = false;
                return Result.success();
            }
        }
    }

    private void uploadStaffFB(List<DailyActivityEntry> staff) {
        for (DailyActivityEntry entry : staff) {
            int inputLength = entry.getEmployeeTimestampIn() != null ? entry.getEmployeeTimestampIn().length() : 0;
            int outputLength = entry.getEmployeeTimestampOut() != null ? entry.getEmployeeTimestampOut().length() : 0;

            Log.d("worker:", " DB correcting outCounter");

            if ((outputLength > 0) && (inputLength == outputLength)) {
                Log.d("worker:", " pushing db to fb");
                mMessagesDatabaseReference.push().setValue(entry);
                updateStaffAvailability(entry.getEmployeeUniqueId(), false);
                dailyActivityDb.dailyActivityDao().deleteEmployee(entry);
            }
        }
        Log.d("worker:", " DB pushed to firebase");
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