package com.tag.management.nfc;

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.snackbar.Snackbar;
import com.tag.management.nfc.engine.EncodeMorseManager;


public class NFCWriteFragment extends DialogFragment {

    static final String TAG = NFCWriteFragment.class.getSimpleName();

    private static final String EMAIL_FAILED = "Email failed";
    private static final String EMAIL_SENT = "Email sent";
    private TextView mTvMessage;
    private LottieAnimationView lottieAnimation;
    private ProgressBar mProgress;
    private Listener mListener;

    static NFCWriteFragment newInstance() {

        return new NFCWriteFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_write, container, false);
        initViews(view);

        GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_write_tag_event), "NFCWriteFragment shown");

        return view;
    }

    private void initViews(View view) {

        mTvMessage = view.findViewById(R.id.tv_message);
        mProgress = view.findViewById(R.id.progress);
        lottieAnimation = view.findViewById(R.id.lottie_read);
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
                String message = getString(R.string.write_tag_error);
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_write_tag_event), message);
                Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
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
                        String label = getString(R.string.rewrite_tag) + message.split("\n")[0];
                        GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_write_tag_event), label);
                        Snackbar.make(this.getView(), label, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                    }
                }

                if (!ndef.isWritable()) {
                    String message = getString(R.string.write_tag_unable);
                    Toast.makeText(getActivity(), message, Toast.LENGTH_LONG).show();
                    GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_write_tag_event), message);
                    ndef.close();
                    return false;
                }

                ndef.writeNdefMessage(ndefMessage);
                ndef.close();
                mTvMessage.setText(getString(R.string.message_write_success));
                lottieAnimation.cancelAnimation();

                return true;
            }

            mTvMessage.setText(getString(R.string.message_write_success));
            lottieAnimation.cancelAnimation();
        } catch (Exception e) {
            Log.e("writeMessage", "Unknown issue: " + e.getMessage());
            mTvMessage.setText(getString(R.string.message_write_error));
            GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_write_tag_event), this.getString(R.string.message_write_error));
            return false;
        }
        mProgress.setVisibility(View.GONE);
        return false;
    }

    private void formatTag(Tag tag, NdefMessage ndefMessage) {
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);

            if (ndefFormatable == null) {
                Toast.makeText(getActivity(), getString(R.string.format_tag_issue), Toast.LENGTH_LONG).show();
            }

            if (ndefFormatable != null) {
                ndefFormatable.connect();
                ndefFormatable.format(ndefMessage);
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_write_tag_event), "tag formatted");
                ndefFormatable.close();
            }

        } catch (Exception e) {
            Log.e("formatTag", e.getMessage());
        }
    }

}
