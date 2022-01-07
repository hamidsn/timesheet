package com.tag.management.nfc;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.airbnb.lottie.LottieAnimationView;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

import java.util.Arrays;
import java.util.List;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import static com.tag.management.nfc.TimesheetUtil.user_id;
import static com.tag.management.nfc.TimesheetUtil.user_name;

public class LauncherActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 1;
    //firebase instance variables
    private LottieAnimationView lottieAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();

        setContentView(R.layout.activity_launcher);
        lottieAnimation = findViewById(R.id.lottie_logo);

        //initialize analytics

        String STARTUP_TRACE_NAME = "nfc_launcher_trace";
        Trace myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();

        ActionBar actionBar = getSupportActionBar();

        createSignInIntent();

    }

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new FirebaseAuthUIActivityResultContract(),
            new ActivityResultCallback<FirebaseAuthUIAuthenticationResult>() {
                @Override
                public void onActivityResult(FirebaseAuthUIAuthenticationResult result) {
                    onSignInResult(result);
                }
            }
    );

    public void createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        List<AuthUI.IdpConfig> providers = Arrays.asList(
                new AuthUI.IdpConfig.EmailBuilder().build(),
                new AuthUI.IdpConfig.GoogleBuilder().build());

        // Create and launch sign-in intent
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setIsSmartLockEnabled(false)
                .setLogo(R.mipmap.ic_launcher)
                .setTosAndPrivacyPolicyUrls(
                        "https://example.com/terms.html",
                        "https://example.com/privacy.html")
                .build();
        signInLauncher.launch(signInIntent);
        // [END auth_fui_create_intent]
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();
        if (result.getResultCode() == RESULT_OK) {
            // Successfully signed in
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_loggedin_event), this.getString(R.string.analytics_loggedin_label));
            Toast.makeText(this, getResources().getString(R.string.signed_in), Toast.LENGTH_SHORT).show();
            if (user != null) {
                // User is signed in
                onSignedInInitialize(user.getDisplayName(), user.getUid());
                GAPAnalytics.instance(this, user.getUid());
                ActionBar actionBar = getSupportActionBar();
                if (actionBar != null) {
                    actionBar.setTitle(user.getDisplayName());
                }
            }
        } else {
            // User is signed out
            GAPAnalytics.instance(this, "");
            if(response!= null){
                if(response.getError()!= null) {
                    Log.d("Signedout", response.getError().getMessage());

                }
            }else {
                Log.d("Signed out", "user canceled the sign-in flow using the back button.");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(lottieAnimation != null) {
            lottieAnimation.playAnimation();
        }
    }

    @Override
    protected void onPause() {
        if(lottieAnimation != null) {
            lottieAnimation.clearAnimation();
        }
        super.onPause();
    }

    private void onSignedInInitialize(String displayName, String uid) {
        TimesheetUtil.setEmployerUid(uid == null ? "" : uid, this);
        TimesheetUtil.setEmployerName(displayName == null ? "" : displayName, this);
    }

    public void onTimesheetClick(View v) {
        Intent intent = new Intent(this, TagReaderActivity.class);
        intent.putExtra(user_name , TimesheetUtil.getEmployerName(this));
        intent.putExtra(user_id , TimesheetUtil.getEmployerUid(this));
        startActivity(intent);
    }

    public void onManageTagsClick(View v) {
        Intent intent = new Intent(this, TagManagementActivity.class);
        intent.putExtra(user_name , TimesheetUtil.getEmployerName(this));
        intent.putExtra(user_id , TimesheetUtil.getEmployerUid(this));
        startActivity(intent);
    }

    public void onReportClick(View v) {
        Intent intent = new Intent(this, ReportActivity.class);
        intent.putExtra(user_name , TimesheetUtil.getEmployerName(this));
        intent.putExtra(user_id , TimesheetUtil.getEmployerUid(this));
        startActivity(intent);
    }

}
