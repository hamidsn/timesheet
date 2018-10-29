package com.tag.management.nfc;

import android.app.DialogFragment;
import android.content.Context;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tag.management.nfc.engine.EncodeMorseManager;

import java.io.IOException;

public class NFCReadFragment extends DialogFragment {

    public static final String TAG = NFCReadFragment.class.getSimpleName();
    private TextView mTvMessage;
    private Listener mListener;

    public static NFCReadFragment newInstance() {

        return new NFCReadFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_read, container, false);
        initViews(view);
        return view;
    }

    private void initViews(View view) {

        mTvMessage = (TextView) view.findViewById(R.id.tv_message);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TagManagementActivity) {
            mListener = (TagManagementActivity) context;
        } else {
            mListener = (TagReaderActivity) context;
        }

        mListener.onDialogDisplayed();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener.onDialogDismissed();
    }

    public void onNfcDetectedManager(Ndef ndef) {

        readFromNFCManager(ndef);
    }

    public void onNfcDetectedStaff(Ndef ndef) {

        readFromNFCStaff(ndef);
    }

    public String returnEmployerName(Ndef ndef) {
        String employer = "";
        try {
            String message = getNdefMessage(ndef);
            if (!TextUtils.isEmpty(message)) {
                Log.d(TAG, "readFromNFCManager: " + message);
                employer = TimesheetUtil.getEmployer(EncodeMorseManager.getEncodedString(message));
            }
            return (employer);

        } catch (IOException | FormatException e) {
            e.printStackTrace();
            return (employer);
        }
    }

    private void readFromNFCStaff(Ndef ndef) {
        try {
            String message = getNdefMessage(ndef);

            if (!TextUtils.isEmpty(message)) {
                Log.d(TAG, "readFromNFCManager: " + message);
                mTvMessage.setText(TimesheetUtil.parseNFCMessageStaff(EncodeMorseManager.getEncodedString(message)));
            } else {
                mTvMessage.setText(R.string.empty_tag);
            }
        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }

    private String getNdefMessage(Ndef ndef) throws IOException, FormatException {
        NdefMessage ndefMessage = null;
        String message = "";
        if (ndef != null) {
            ndef.connect();
            ndefMessage = ndef.getNdefMessage();
            if (ndefMessage != null) {
                message = new String(ndefMessage.getRecords()[0].getPayload());
            }
        }
        ndef.close();
        return message;
    }

    private void readFromNFCManager(Ndef ndef) {
        try {
            String message = getNdefMessage(ndef);

            if (!TextUtils.isEmpty(message)) {
                Log.d(TAG, "readFromNFCManager: " + message);
                mTvMessage.setText(TimesheetUtil.parseNFCMessageManager(EncodeMorseManager.getEncodedString(message)));
            } else {
                mTvMessage.setText(R.string.empty_tag);
            }
        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }
}
