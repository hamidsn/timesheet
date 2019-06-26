package com.tag.management.nfc;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.creativityapps.gmailbackgroundlibrary.BackgroundMail;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.tag.management.nfc.engine.EncodeMorseManager;

import java.util.HashMap;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class NFCWriteFragment extends DialogFragment {

    static final String TAG = NFCWriteFragment.class.getSimpleName();
    private static final String SEND_EMAIL = "send_email";
    //public static final String UTF_8 = "UTF-8";
    private static final String EMAIL_FAILED = "Email failed";
    private static final String EMAIL_SENT = "Email sent";
    private TextView mTvMessage;
    private ProgressBar mProgress;
    private Listener mListener;
    private boolean send_email = false;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    static NFCWriteFragment newInstance() {

        return new NFCWriteFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write, container, false);
        initViews(view);
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();

        // Create Remote Config Setting to enable developer mode.
        // Fetching configs from the server is normally limited to 5 requests per hour.
        // Enabling developer mode allows many more requests to be made per hour, so developers
        // can test different config values during development.
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        // Define default config values. Defaults are used when fetched config values are not
        // available. Eg: if an error occurred fetching values from the server.
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(SEND_EMAIL, send_email);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        fetchConfig();

        return view;
    }

    private void initViews(View view) {

        mTvMessage = view.findViewById(R.id.tv_message);
        mProgress = view.findViewById(R.id.progress);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (TagManagementActivity) context;
        mListener.onDialogDisplayed();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.onDialogDismissed();
    }

    boolean onNfcDetected(Tag tag, NdefMessage ndefMessage, String emailAddress, String messageToWrite, String employerName) {

        mProgress.setVisibility(View.VISIBLE);
        try {
            if (tag == null) {
                Toast.makeText(getActivity(), "An Error has Occurred, tag in null. Please Try Again", Toast.LENGTH_LONG).show();
            }

            Ndef ndef = Ndef.get(tag);

            if (ndef == null) {
                formatTag(tag, ndefMessage);
            } else {
                ndef.connect();

                ///check if tag is already registered, then delete the old message
                NdefMessage ndefOldMessage = ndef.getNdefMessage();
                if (ndefOldMessage != null) {


                    String message = new String(ndefOldMessage.getRecords()[0].getPayload());
                    message = EncodeMorseManager.getEncodedString(message);
                    if (this.getView() != null) {
                        Snackbar.make(this.getView(), "Rewriting tag. Older name was " + message.split("\n")[0], Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }

                if (!ndef.isWritable()) {
                    Toast.makeText(getActivity(), "Tag is not Writable", Toast.LENGTH_LONG).show();
                    ndef.close();
                    return false;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                mTvMessage.setText(getString(R.string.message_write_success));
                if (send_email) {
                    sendEmail(emailAddress, messageToWrite + " " + employerName);
                }
                return true;
            }

            mTvMessage.setText(getString(R.string.message_write_success));
        } catch (Exception e) {
            Log.e("writeMessage", "Unknown issue: " + e.getMessage());
            mTvMessage.setText(getString(R.string.message_write_error));
            Toast.makeText(getActivity(), "Unknown issue: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
        mProgress.setVisibility(View.GONE);
        return false;
    }

    private void sendEmail(String emailAddress, String body) {

        if (TextUtils.isEmpty(body)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.empty_tag), Toast.LENGTH_SHORT).show();
        } else {
            if (this.getContext() != null) {
                BackgroundMail.newBuilder(this.getContext())
                        .withUsername(getResources().getString(R.string.email_address))
                        .withPassword(getResources().getString(R.string.email_pass))
                        .withMailto(emailAddress)
                        .withType(BackgroundMail.TYPE_PLAIN)
                        .withSubject(getResources().getString(R.string.email_title))
                        //todo: create a good message
                        .withBody(body).withProcessVisibility(false)
                        .withOnSuccessCallback(() -> Toast.makeText(getActivity(), EMAIL_SENT, Toast.LENGTH_LONG).show())
                        .withOnFailCallback(() -> Toast.makeText(getActivity(), EMAIL_FAILED, Toast.LENGTH_LONG).show())
                        .send();
            }

        }
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(getActivity(), "Tag is not formatable", Toast.LENGTH_LONG).show();
            }

            if (ndefFormatable != null) {
                ndefFormatable.connect();
                ndefFormatable.format(ndefMessage);
                ndefFormatable.close();
            }

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }
    }

    // Fetch the config to determine the allowed send_email.
    private void fetchConfig() {
        long cacheExpiration = 3600; // 1 hour in seconds
        if (mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
                .addOnSuccessListener(aVoid -> {
                    mFirebaseRemoteConfig.activateFetched();
                    applyRetrievedLengthLimit();
                })
                .addOnFailureListener(e -> {
                    // An error occurred when fetching the config.
                    Log.w(TAG, "Error fetching config", e);
                    applyRetrievedLengthLimit();
                });
    }

    /**
     * Apply retrieved sending email. This result may be fresh from the server or it may be from
     * cached values.
     */
    private void applyRetrievedLengthLimit() {
        send_email = mFirebaseRemoteConfig.getBoolean(SEND_EMAIL);
        Log.d(TAG, SEND_EMAIL + " = " + String.valueOf(send_email));
    }
}
