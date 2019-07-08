package com.tag.management.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.util.Arrays;
import java.util.List;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import io.fabric.sdk.android.Fabric;

public class LauncherActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;
    //firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_launcher);

        //initialize analytics

        mFirebaseAuth = FirebaseAuth.getInstance();

        String STARTUP_TRACE_NAME = "nfc_launcher_trace";
        Trace myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();
        Fabric.with(this, new Crashlytics());
        ActionBar actionBar = getSupportActionBar();

        mAuthStateListener = firebaseAuth -> {
            List<AuthUI.IdpConfig> providers = Arrays.asList(
                    new AuthUI.IdpConfig.EmailBuilder().build(),
                    new AuthUI.IdpConfig.GoogleBuilder().build());

            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user != null) {
                // User is signed in
                onSignedInInitialize(user.getUid());
                GAPAnalytics.instance(this, user.getUid());
                if (actionBar != null) {
                    actionBar.setTitle(user.getDisplayName());
                }
            } else {
                // User is signed out
                GAPAnalytics.instance(this, "");
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
        mFirebaseAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        if (mAuthStateListener != null) {
            mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        }
        super.onPause();
    }

    private void onSignedInInitialize(String uid) {
        TimesheetUtil.setEmployerUid(uid, this);
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

}
