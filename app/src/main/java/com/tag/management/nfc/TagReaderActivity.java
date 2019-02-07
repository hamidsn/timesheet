package com.tag.management.nfc;

import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
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
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.tag.management.nfc.database.AppDatabase;
import com.tag.management.nfc.database.DailyActivityDatabase;
import com.tag.management.nfc.database.DailyActivityEntry;
import com.tag.management.nfc.database.EmployeeEntry;
import com.tag.management.nfc.engine.AppExecutors;
import com.tag.management.nfc.model.Employee;
import com.tag.management.nfc.model.MainViewModel;

import java.util.Arrays;
import java.util.List;

import io.fabric.sdk.android.Fabric;

import static android.support.v7.widget.DividerItemDecoration.VERTICAL;

public class TagReaderActivity extends AppCompatActivity implements Listener, StaffListener, EmployeeAdapter.ItemClickListener {

    public static final String READ_FROM_NFC = "readFromNfc";
    //public static final String DATA = "data";
    public static final String EMPLOYEES = "employees";
    public static final String INSTANCE_TASK_ID = "instanceTaskId";
    private static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 1;
    // Constant for default task id to be used when not in update mode
    private static final int DEFAULT_TASK_ID = -1;
    private final String STARTUP_TRACE_NAME = "nfc_launcher_trace";
    private final String ERROR = "bad_data";
    //firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private Trace myTrace;
    private String employerUid, employerName;
    private NFCReadFragment mNfcReadFragment;
    private boolean isDialogDisplayed = false;
    private NfcAdapter mNfcAdapter;
    private String appEmployer;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private int mTaskId = DEFAULT_TASK_ID;

    private EmployeeAdapter mAdapter;

    private AppDatabase employeeListDb;
    private DailyActivityDatabase dailyActivityDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tag_reader);

        initView();

        if (savedInstanceState != null && savedInstanceState.containsKey(INSTANCE_TASK_ID)) {
            mTaskId = savedInstanceState.getInt(INSTANCE_TASK_ID, DEFAULT_TASK_ID);
        }

        initNFC();

        initFirebase();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(INSTANCE_TASK_ID, mTaskId);
        super.onSaveInstanceState(outState);
    }

    private void initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    private void initView() {
        RecyclerView mRecyclerView = findViewById(R.id.recyclerViewTasks);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new EmployeeAdapter(this, this);
        mRecyclerView.setAdapter(mAdapter);
        DividerItemDecoration decoration = new DividerItemDecoration(getApplicationContext(), VERTICAL);
        mRecyclerView.addItemDecoration(decoration);

        employeeListDb = AppDatabase.getInstance(getApplicationContext());
        dailyActivityDb = DailyActivityDatabase.getInstance(getApplicationContext());

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> showReadFragment());
        setupViewModel();
    }

    private void setupViewModel() {
        MainViewModel viewModel = ViewModelProviders.of(this).get(MainViewModel.class);
        viewModel.getEmployees().observe(this, employeeEntries -> {
            Log.d("TAG", "Updating list of tasks from LiveData in ViewModel");
            mAdapter.setTasks(employeeEntries);
        });
    }

    private void showReadFragment() {
        mNfcReadFragment = (NFCReadFragment) getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
        if (mNfcReadFragment == null) {
            mNfcReadFragment = NFCReadFragment.newInstance();
        }
        mNfcReadFragment.show(getFragmentManager(), NFCReadFragment.TAG);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {

            if (isDialogDisplayed) {
                myTrace.incrementCounter(READ_FROM_NFC);
                mNfcReadFragment = (NFCReadFragment) getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
                Ndef ndef = Ndef.get(tag);

                String tagEmployer = mNfcReadFragment.returnEmployerName(ndef);
                if (appEmployer.toLowerCase().equals(tagEmployer.toLowerCase())) {
                    mNfcReadFragment.onNfcDetectedStaff(ndef);

                } else {
                    Toast.makeText(this, "Sorry, this tag is for " + (tagEmployer.length() > 1 ? tagEmployer : "another business") + ". We work for " + appEmployer, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void initFirebase() {
        Fabric.with(this, new Crashlytics());
        mFirebaseAuth = FirebaseAuth.getInstance();

        //todo: are we used?
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
        myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();
        Fabric.with(this, new Crashlytics());

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(EMPLOYEES);

        mAuthStateListener = firebaseAuth -> {

            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                onSignedInInitialize(user.getDisplayName(), user.getUid());
                appEmployer = user.getDisplayName();
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

    @Override
    public void onDialogDisplayed() {
        isDialogDisplayed = true;
    }

    @Override
    public void onDialogDismissed() {
        isDialogDisplayed = false;
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
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
    }

    @Override
    protected void onPause() {
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    private void onSignedInInitialize(String username, String uid) {
        employerName = username;
        employerUid = uid;
        TimesheetUtil.setEmployerUid(uid, this);
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(employerUid);
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, String s) {
                Employee employee = dataSnapshot.getValue(Employee.class);
                final EmployeeEntry employeeIndividual;
                if (employee != null) {
                    employeeIndividual = new EmployeeEntry(employee.getEmployerName(), employee.getEmployeeFullName(), employee.getEmployerUid(), employee.getEmployeeDownloadUrl(), employee.getEmployeeEmail(), employee.getEmployeeUniqueId(), false);
                } else {
                    employeeIndividual = new EmployeeEntry(ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, false);
                }
                AppExecutors.getInstance().diskIO().execute(() -> {

                    // check if employee is not in DB then add it
                    if (employeeListDb.employeeDao().loadEmployeeByUid(employeeIndividual.getEmployeeUniqueId()) == null) {
                        employeeListDb.employeeDao().insertEmployee(employeeIndividual);
                    }
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
    }

    private void detachDatabaseReadListener() {
        if (mChildEventListener != null) {
            mMessagesDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    @Override
    public void onItemClickListener(String itemId) {
        // Launch AddTaskActivity adding the itemId as an extra in the intent
        //todo, change manual
    }

    private void updateStaffAvailability(String uId, boolean availability) {
        if (employeeListDb.employeeDao().loadEmployeeByUid(uId) != null) {
            EmployeeEntry employee;
            employee = employeeListDb.employeeDao().loadEmployeeByUid(uId);
            employee.setEmployeeAvailable(availability);
            employeeListDb.employeeDao().updateEmployee(employee);
        }
    }

    @Override
    public void onStaffDetails(String name, String uId, String employer) {
        Log.d("TAG", name);

        //employeeIndividual = new DailyActivityEntry(employer, name,   TimesheetUtil.getCurrentTimeUsingCalendar(), TimesheetUtil.getCurrentTimeUsingCalendar(), uId);

        AppExecutors.getInstance().diskIO().execute(() -> {
            DailyActivityEntry employeeIndividual = null;
            boolean availability = false;
            if (dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId) == null) {
                //new start staff
                employeeIndividual = new DailyActivityEntry(employer, name, TimesheetUtil.getCurrentTimeUsingCalendar(), "", uId);
                availability = true;

            } else if (TextUtils.isEmpty(dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampOut())) {
                //finishing staff
                employeeIndividual = new DailyActivityEntry(employer, name, dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampIn(), TimesheetUtil.getCurrentTimeUsingCalendar(), uId);
                dailyActivityDb.dailyActivityDao().deleteEmployee(dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId));
                availability = false;

            } else if (!TextUtils.isEmpty(dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampIn()) && !TextUtils.isEmpty(dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampOut())) {
                //returning after break
                int inCounter = dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampIn().split("-").length;
                int outCounter = dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampOut().split("-").length;

                if (inCounter > outCounter) {
                    // returning finishing
                    employeeIndividual = new DailyActivityEntry(employer, name, dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampIn(), dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampOut() + "-" + TimesheetUtil.getCurrentTimeUsingCalendar(), uId);
                    availability = false;
                } else {
                    // returning start
                    employeeIndividual = new DailyActivityEntry(employer, name, dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampIn() + "-" + TimesheetUtil.getCurrentTimeUsingCalendar(), dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId).getEmployeeTimestampOut(), uId);
                    availability = true;
                }
                dailyActivityDb.dailyActivityDao().deleteEmployee(dailyActivityDb.dailyActivityDao().loadEmployeeByUid(uId));
            }
            updateStaffAvailability(uId, availability);
            dailyActivityDb.dailyActivityDao().insertEmployee(employeeIndividual);
        });

    }
}
