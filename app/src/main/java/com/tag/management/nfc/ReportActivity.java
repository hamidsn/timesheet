package com.tag.management.nfc;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.savvi.rangedatepicker.CalendarPickerView;
import com.tag.management.nfc.database.ReportDatabase;
import com.tag.management.nfc.database.ReportEntry;
import com.tag.management.nfc.engine.AppExecutors;
import com.tag.management.nfc.model.Report;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import io.fabric.sdk.android.Fabric;

public class ReportActivity extends AppCompatActivity {

    public static final String CALENDAR_DIVIDER = " / ";
    public static final String MONTH_YEAR_PICKER_DIALOG = "MonthYearPickerDialog";
    private static final String EMPLOYEES = "employees";
    private static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 1;
    private final String ERROR = "bad_data";
   // Handler handler;
    //firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private Trace myTrace;
    private String STARTUP_TRACE_NAME = "nfc_launcher_trace";
    private String employerUid, employerName;
    private DatabaseReference mMessagesDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private String selectedYear = "";
    private String selectedMonth = "";
    private String selectedDay = "";
    private ReportDatabase reportListDb;
    private String READ_REPORT_START = "readReportStart";
    private String READ_REPORT_FINISH = "readReportFinish";
    private String PARSEINFORMATION = "parseInformation";
    List<ReportEntry> staff = null;
    List<ReportEntry> mFinalList = new ArrayList<>();
    int staffNumber = 0;
    int downloadedDay = 0;
    int maxSelectedDays = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //handler = new Handler();
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_report);

        initView();
        initFirebase();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        super.onPause();
    }

    private void onSignedInInitialize(String username, String uid) {
        employerName = username;
        employerUid = uid;

    }

    private void onSignedOutCleanup() {
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
        detachDatabaseReadListener();
    }


    private void initFirebase() {
        mFirebaseAuth = FirebaseAuth.getInstance();

        //todo: are we used?
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
        myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();
        Fabric.with(this, new Crashlytics());

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        // mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(EMPLOYEES);

        mAuthStateListener = firebaseAuth -> {
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                onSignedInInitialize(user.getDisplayName(), user.getUid());
            } else {
                // User is signed out
                onSignedOutCleanup();
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setIsSmartLockEnabled(false)
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
        };
    }

    private void initView() {
        //EditText etPickADate;
        CalendarPickerView calendar = findViewById(R.id.calendar_view);

        Calendar pastYear = Calendar.getInstance();
        pastYear.add(Calendar.YEAR, -2);
        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.DATE, 1);


        calendar.init(pastYear.getTime(), nextYear.getTime()) //
                .inMode(CalendarPickerView.SelectionMode.RANGE)
                .withSelectedDate(new Date());
        calendar.setTypeface(Typeface.SANS_SERIF);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
                {

                    // todo delete old report?
                    AppExecutors.getInstance().diskIO().execute(() -> reportListDb.reportDao().nukeTable());
                    staffNumber = 0;
                    downloadedDay = 0;

                    myTrace.incrementMetric(READ_REPORT_START, 1);
                    Snackbar.make(view, "Reading data from server", Snackbar.LENGTH_LONG)
                            .setAction("Remote handshake", null).show();
                    List<Date> selectedDays = calendar.getSelectedDates();

                    SimpleDateFormat yFormat = new SimpleDateFormat("yyyy");
                    SimpleDateFormat mFormat = new SimpleDateFormat("MM");
                    SimpleDateFormat dFormat = new SimpleDateFormat("dd");

                    maxSelectedDays = selectedDays.size();
                    for (int dayCounter = 0; dayCounter < maxSelectedDays; dayCounter++) {
                        selectedYear = yFormat.format(selectedDays.get(dayCounter));
                        selectedMonth = mFormat.format(selectedDays.get(dayCounter));
                        selectedDay = dFormat.format(selectedDays.get(dayCounter));
                        if (!TextUtils.isEmpty(employerUid)) {
                            mMessagesDatabaseReference = mFirebaseDatabase.getReference()
                                    .child(selectedYear)
                                    .child(selectedMonth)
                                    .child(selectedDay)
                                    .child(employerUid);
                            attachDatabaseReadListener(dayCounter, maxSelectedDays);
                        }
                    }
                }
        );

        reportListDb = ReportDatabase.getInstance(getApplicationContext());

    }

    private void attachDatabaseReadListener(int day, int maxDays) {
        if (day == 0) {
            showSpinner();
        }
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                try {
                    Report report = dataSnapshot.getValue(Report.class);
                    final ReportEntry employeeIndividual;
                    if (report != null) {
                        employeeIndividual = new ReportEntry(report.getEmployeeFullName(), report.getEmployeeTimestampIn(), report.getEmployeeTimestampOut(), report.getEmployeeUniqueId(), report.getEmployerName(), report.getId());
                        Log.d("report", "* " + report.getEmployeeFullName());
                        Log.d("report", "* " + report.getId());
                        Log.d("report", "* " + report.getEmployeeTimestampIn());
                        Log.d("report", "* " + report.getEmployeeTimestampOut());
                        Log.d("report", "******\n" );

                    } else {
                        employeeIndividual = new ReportEntry(ERROR, ERROR, ERROR, ERROR, ERROR, 0);
                    }

                        // check if employee is not in DB then add it
                        AppExecutors.getInstance().diskIO().execute(() -> {

                            if (!TextUtils.isEmpty(employeeIndividual.getEmployerName())) {
                                reportListDb.reportDao().insertReport(employeeIndividual);
                            }

                        });
                } catch (DatabaseException e) {
                    Log.e("report", e.getMessage());
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        };
        mMessagesDatabaseReference.addChildEventListener(mChildEventListener);

        mMessagesDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                System.out.println("We're done loading the initial "+dataSnapshot.getChildrenCount()+" items");
                downloadedDay += 1;
                staffNumber +=dataSnapshot.getChildrenCount();
                Log.d("report", "staffNumber:" + staffNumber);
                Log.d("report", "downloadedDay:" + downloadedDay);
                Log.d("report", "maxSelectedDays:" + maxSelectedDays);
                if(downloadedDay == maxSelectedDays){
                    //we finished downloading the last selected day
                    parseInfo();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        mMessagesDatabaseReference.keepSynced(false);

        if (day == maxDays - 1) {
            myTrace.incrementMetric(READ_REPORT_FINISH, 1);
            hideSpinner();

        }
    }

    private void showSpinner() {
        //10 seconds spinner to make sure data is downloaded from firebase
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);

       /* handler.postDelayed(() -> {
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            parseInfo();
        }, 10000);*/

    }

    private void hideSpinner() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        /*if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }*/

    }

    private void parseInfo() {
        myTrace.incrementMetric(PARSEINFORMATION, 1);
        AppExecutors.getInstance().diskIO().execute(() -> {
                    staff = reportListDb.reportDao().loadAllReports();
                    Log.d("report", "* ***** * final staff number" + staff.size());

            mFinalList = TimesheetUtil.filterStaffTimetable(staff);
            if(mFinalList.isEmpty()){
                showWrongMessage(this);
            } else {
                TimesheetUtil.createHTML(this, mFinalList, employerUid);
            }
        });

        //todo Create html file based on mFinalList
        //todo store in html file name
        //todo create folder in fb by employerid
        //todo upload html
        //todo show page, share link or print html
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myTrace.stop();
    }

    private void showWrongMessage(final Context context)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(
                () -> Toast.makeText(context, "No entry found in cloud. Try other dates", Toast.LENGTH_SHORT).show()
        );
    }
}
