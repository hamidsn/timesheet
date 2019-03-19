package com.tag.management.nfc;

import android.content.Intent;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.tag.management.nfc.database.ReportDatabase;
import com.tag.management.nfc.database.ReportEntry;
import com.tag.management.nfc.engine.AppExecutors;
import com.tag.management.nfc.model.Report;
import com.tag.management.nfc.views.MonthYearPickerDialog;

import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;

import io.fabric.sdk.android.Fabric;

public class ReportActivity extends AppCompatActivity {

    public static final String CALENDAR_DIVIDER = " / ";
    public static final String MONTH_YEAR_PICKER_DIALOG = "MonthYearPickerDialog";
    private static final String EMPLOYEES = "employees";
    private static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 1;
    private final String ERROR = "bad_data";
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
    private ReportDatabase reportListDb;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
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
        EditText etPickADate;


        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        reportListDb = ReportDatabase.getInstance(getApplicationContext());

        MonthYearPickerDialog pickerDialog = new MonthYearPickerDialog();
        etPickADate = findViewById(R.id.et_datePicker);
        etPickADate.setOnClickListener(arg0 -> pickerDialog.show(getSupportFragmentManager(), MONTH_YEAR_PICKER_DIALOG));

        pickerDialog.setListener((view, year, month, dayOfMonth) -> {
            selectedYear = Integer.toString(year);
            selectedMonth = Integer.toString(month);
            if (month < 10) {
                selectedMonth = "0" + selectedMonth;
            }
            AppExecutors.getInstance().diskIO().execute(() -> {
                reportListDb.clearAllTables();
            });
            findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            if(handler != null ){
                handler.removeCallbacksAndMessages(null);
            }

            if (!TextUtils.isEmpty(employerUid)) {
                for (int i = 1; i < 32; i++) {
                    String day = Integer.toString(i);
                    if (i < 10) {
                        day = "0" + day;
                    }
                    mMessagesDatabaseReference = mFirebaseDatabase.getReference()
                            .child(selectedYear)
                            .child(selectedMonth)
                            .child(day)
                            .child(employerUid);

                    //readDB(i);
                    attachDatabaseReadListener(i);
                }
            }

            etPickADate.setText(new StringBuilder()
                    .append(TimesheetUtil.getMonth(month)).append(CALENDAR_DIVIDER).append(selectedYear).append(" "));
        });

    }

 /*   private void readDB(int day) {
        mMessagesDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Report report = dataSnapshot.getValue(Report.class);

               // DataSnapshot fdfd = dataSnapshot.child(dataSnapshot.getKey());

                final ReportEntry employeeIndividual;
                if (report != null) {
                    employeeIndividual = new ReportEntry(report.getEmployeeFullName(), report.getEmployeeTimestampIn(), report.getEmployeeTimestampOut(), report.getEmployeeUniqueId(), report.getEmployerName(), report.getId());
                Log.d("report", "" + report.getEmployeeFullName());
                } else {
                    employeeIndividual = new ReportEntry(ERROR, ERROR, ERROR, ERROR, ERROR, 0);
                }

                AppExecutors.getInstance().diskIO().execute(() -> {

                    // check if employee is not in DB then add it
                    if(employeeIndividual.getEmployeeTimestampIn() != null && !employeeIndividual.getEmployeeTimestampIn().equals(ERROR)){
                        reportListDb.reportDao().insertReport(employeeIndividual);
                    }

                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }*/


    private void attachDatabaseReadListener(int day) {
        //todo read fbdb of the day, store it in a local db
        //it is an array for all days of a month

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {

                    Report report = dataSnapshot.getValue(Report.class);

                final ReportEntry employeeIndividual;
                if (report != null) {
                    employeeIndividual = new ReportEntry(report.getEmployeeFullName(), report.getEmployeeTimestampIn(), report.getEmployeeTimestampOut(), report.getEmployeeUniqueId(), report.getEmployerName(), report.getId());
                    Log.d("report-", "" + report.getEmployeeFullName());
                } else {
                    employeeIndividual = new ReportEntry(ERROR, ERROR, ERROR, ERROR, ERROR, 0);
                }

                AppExecutors.getInstance().diskIO().execute(() -> {

                    // check if employee is not in DB then add it
                    AppExecutors.getInstance().diskIO().execute(() -> {

                        if(!TextUtils.isEmpty(employeeIndividual.getEmployerName())){
                            reportListDb.reportDao().insertReport(employeeIndividual);
                        }

                    });
                });
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
        mMessagesDatabaseReference.keepSynced(false);

        if(day == 31){
            showSpinner();
        }
    }

    private void showSpinner() {
        //10 seconds spinner to make sure data is downloaded from firebase
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);


        handler.postDelayed(new Runnable() {
            public void run() {
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
                parseInfo();
            }
        }, 10000);

    }

    private void parseInfo() {

    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }


}
