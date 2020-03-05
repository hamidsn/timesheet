package com.tag.management.nfc;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.fabric.sdk.android.Fabric;

public class ReportActivity extends AppCompatActivity {

    public static final String CALENDAR_DIVIDER = "/";
    private static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 1;
    private final String ERROR = "bad_data";
    List<ReportEntry> staff = null;
    List<ReportEntry> mFinalList = new ArrayList<>();
    int staffNumber = 0;
    int downloadedDay = 0;
    int maxSelectedDays = 0;
    private FirebaseAuth mFirebaseAuth;
    private ChildEventListener mChildEventListener;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private Trace myTrace;
    private String employerUid, employerName;
    private DatabaseReference mMessagesDatabaseReference;
    private FirebaseDatabase mFirebaseDatabase;
    private String selectedYear = "";
    private String firstSelectedYear = "";
    private String lastSelectedYear = "";
    private String selectedMonth = "";
    private String selectedDay = "";
    private String startDate, endDate;
    private ReportDatabase reportListDb;
    private FloatingActionButton fab;
    private Date firstSelectedDay;
    private Date lastSelectedDay;

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
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_loggedin_event), this.getString(R.string.analytics_loggedin_label));
                Toast.makeText(this, getResources().getString(R.string.signed_in), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSpinner();
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
        String STARTUP_TRACE_NAME = "nfc_launcher_trace";
        myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();
        Fabric.with(this, new Crashlytics());

        mFirebaseDatabase = FirebaseDatabase.getInstance();

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
        CalendarPickerView calendar = findViewById(R.id.calendar_view);
        Calendar pastYear = Calendar.getInstance();
        pastYear.add(Calendar.YEAR, -2);
        Calendar nextYear = Calendar.getInstance();
        nextYear.add(Calendar.DATE, 1);

        calendar.init(pastYear.getTime(), nextYear.getTime())
                .inMode(CalendarPickerView.SelectionMode.RANGE)
                .withSelectedDate(new Date());

        calendar.setTypeface(Typeface.SANS_SERIF);

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(view ->
                {
                    if (!TimesheetUtil.isNetworkAvailable(this)) {
                        showSnackMessage(this, "Network connection error", false, R.color.snackbar_error);
                    } else {

                        showSpinner();
                        AppExecutors.getInstance().diskIO().execute(() -> reportListDb.reportDao().nukeTable());
                        staffNumber = 0;
                        downloadedDay = 0;

                        myTrace.incrementMetric("readReportStart", 1);

                        showSnackMessage(this, "Reading data from server", false, R.color.snackbar_success);
                        List<Date> selectedDays = calendar.getSelectedDates();
                        // adding days to before and after selected days
                        Date extraDates;
                        Calendar c = Calendar.getInstance();
                        firstSelectedDay = selectedDays.get(0);
                        lastSelectedDay = selectedDays.get(selectedDays.size() - 1);

                        // read 15 days before and after and search for selected dates
                        for (int i = 1; i < 15; i++) {
                            //start searching from 14 days ago
                            c.setTime(firstSelectedDay);
                            c.add(Calendar.DATE, -i);
                            extraDates = c.getTime();
                            selectedDays.add(0, extraDates);
                            //finish at 14 days after
                            c.setTime(lastSelectedDay);
                            c.add(Calendar.DATE, i);
                            extraDates = c.getTime();
                            selectedDays.add(extraDates);
                        }

                        @SuppressLint("SimpleDateFormat") SimpleDateFormat yFormat = new SimpleDateFormat("yyyy", Locale.ENGLISH);
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat mFormat = new SimpleDateFormat("MM", Locale.ENGLISH);
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat dFormat = new SimpleDateFormat("dd", Locale.ENGLISH);
                        selectedYear = yFormat.format(firstSelectedDay);
                        firstSelectedYear = selectedYear;
                        selectedMonth = mFormat.format(firstSelectedDay);
                        selectedDay = dFormat.format(firstSelectedDay);
                        startDate = selectedDay + CALENDAR_DIVIDER + selectedMonth + CALENDAR_DIVIDER + selectedYear;
                        selectedYear = yFormat.format(lastSelectedDay);
                        lastSelectedYear = selectedYear;
                        selectedMonth = mFormat.format(lastSelectedDay);
                        selectedDay = dFormat.format(lastSelectedDay);
                        endDate = selectedDay + CALENDAR_DIVIDER + selectedMonth + CALENDAR_DIVIDER + selectedYear;

                        maxSelectedDays = selectedDays.size();
                        //ignore 14 daysart added days from beginning and end.
                        if (!yFormat.format(selectedDays.get(14)).equals(yFormat.format(selectedDays.get(maxSelectedDays - 15)))) {
                            showSnackMessage(this, "Sorry, report from two different years are not accepted.", true, R.color.snackbar_error);
                        } else {
                            GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_selected_dates_event), "from: " + startDate + " to: " + endDate);
                            for (int dayCounter = 0; dayCounter < maxSelectedDays; dayCounter++) {
                                selectedYear = yFormat.format(selectedDays.get(dayCounter));
                                selectedMonth = mFormat.format(selectedDays.get(dayCounter));
                                selectedDay = dFormat.format(selectedDays.get(dayCounter));

                                if (!TextUtils.isEmpty(employerUid) && (firstSelectedYear.equals(selectedYear) && lastSelectedYear.equals(selectedYear))) {
                                    mMessagesDatabaseReference = mFirebaseDatabase.getReference()
                                            .child(selectedYear)
                                            .child(selectedMonth)
                                            .child(selectedDay)
                                            .child(employerUid);
                                    attachDatabaseReadListener(dayCounter, maxSelectedDays);
                                }
                            }
                        }
                    }
                }
        );

        reportListDb = ReportDatabase.getInstance(getApplicationContext());

    }

    private void attachDatabaseReadListener(int day, int maxDays) {

        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                try {
                    Report report = dataSnapshot.getValue(Report.class);
                    final ReportEntry employeeIndividual;
                    if (report != null) {
                        employeeIndividual = new ReportEntry(report.getEmployeeFullName(), report.getEmployeeTimestampIn(), report.getEmployeeTimestampOut(), report.getEmployeeUniqueId(), report.getEmployerName(), report.getId());

                    } else {
                        employeeIndividual = new ReportEntry(ERROR, ERROR, ERROR, ERROR, ERROR, 0);
                    }
                    Timestamp downloadedTime = TimesheetUtil.convertStringToTimestamp(employeeIndividual.getEmployeeTimestampIn(), firstSelectedYear);

                    // check if employee is not in DB then add it
                    AppExecutors.getInstance().diskIO().execute(() -> {
                        Calendar c = Calendar.getInstance();
                        c.setTime(firstSelectedDay);
                        int firstDay = c.get(Calendar.DAY_OF_MONTH);
                        c.setTime(lastSelectedDay);
                        int lastDay = c.get(Calendar.DAY_OF_MONTH);
                        c.setTimeInMillis(downloadedTime.getTime());
                        int downloadDay = c.get(Calendar.DAY_OF_MONTH);

                        if (!TextUtils.isEmpty(employeeIndividual.getEmployerName())
                                && (downloadedTime.after(firstSelectedDay) || firstDay == downloadDay)
                                && (downloadedTime.before(lastSelectedDay) || lastDay == downloadDay)) {
                            reportListDb.reportDao().insertReport(employeeIndividual);
                        }

                    });
                } catch (DatabaseException | ParseException e) {
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
                Log.d("onDataChange", "We're done loading the initial " + dataSnapshot.getChildrenCount() + " items");
                downloadedDay += 1;
                staffNumber += dataSnapshot.getChildrenCount();
                if (downloadedDay == maxSelectedDays) {
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
            myTrace.incrementMetric("readReportFinish", 1);
            //hideSpinner();

        }
    }

    @SuppressLint("RestrictedApi")
    private void showSpinner() {
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        fab.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("RestrictedApi")
    private void hideSpinner() {
        findViewById(R.id.loadingPanel).setVisibility(View.GONE);
        fab.setVisibility(View.VISIBLE);
    }

    private void parseInfo() {
        GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_parse_info_event), "parsing downloaded info of" + (staff != null ? staff.size() : 0));
        myTrace.incrementMetric("parseInformation", 1);
        AppExecutors.getInstance().diskIO().execute(() -> {
            staff = reportListDb.reportDao().loadAllReports();

            mFinalList = TimesheetUtil.filterStaffTimetable(staff);
            if (mFinalList.isEmpty()) {
                showSnackMessage(this, "No entry found in cloud. Try other dates", true, R.color.snackbar_error);

            } else {
                TimesheetUtil.createHTML(this, mFinalList, employerUid, startDate, endDate, employerName);
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_html_report_event), "htnl created for: " + employerUid);

                showSnackMessage(this, "Creating report in the cloud", false, R.color.snackbar_success);
            }
        });
        showSpinner();

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

    private void showSnackMessage(final Context context, String message, boolean hideSpinner, int color) {
        GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_snack_message_event), message);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(
                () -> {
                    //Toast.makeText(context, "No entry found in cloud. Try other dates", Toast.LENGTH_SHORT).show();
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
                            .setAction("Remote handshake", null);
                    View snackBarView = snackbar.getView();
                    snackBarView.setBackgroundColor(ContextCompat.getColor(context, color));
                    snackbar.show();
                    if (hideSpinner) {
                        hideSpinner();
                    }
                }
        );

    }
}
