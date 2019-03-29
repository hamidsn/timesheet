package com.tag.management.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import io.fabric.sdk.android.Fabric;


public class LauncherActivity extends AppCompatActivity {

    private static final String ANONYMOUS = "anonymous";
    private static final int RC_SIGN_IN = 1;
    //firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    private String employerUid, employerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_launcher);

        mFirebaseAuth = FirebaseAuth.getInstance();

        //todo: are we used?
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
        String STARTUP_TRACE_NAME = "nfc_launcher_trace";
        Trace myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();
        Fabric.with(this, new Crashlytics());

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        Toast.makeText(this, "worker scheduled: " + isWorkScheduled("HAMID"), Toast.LENGTH_SHORT).show();

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
        TimesheetUtil.setEmployerUid(uid, this);
    }

    private void onSignedOutCleanup() {
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
    }

    public void onTimesheetClick(View v) {
        Intent intent = new Intent(this, TagReaderActivity.class);
        startActivity(intent);
    }

    public void onManageTagsClick(View v) {
        Intent intent = new Intent(this, TagManagementActivity.class);
        startActivity(intent);
    }

    public void onReportClick(View v) {
        Intent intent = new Intent(this, ReportActivity.class);
        startActivity(intent);
    }

//testing
    private boolean isWorkScheduled(String tag) {
        WorkManager instance = WorkManager.getInstance();
        ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosByTag(tag);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = (state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED) | running;
            }
            return running;
        } catch (ExecutionException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

}
