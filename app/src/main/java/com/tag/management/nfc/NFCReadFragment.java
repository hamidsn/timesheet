package com.tag.management.nfc;

import android.content.Context;
import android.graphics.Color;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.tag.management.nfc.database.AppDatabase;
import com.tag.management.nfc.engine.AppExecutors;
import com.tag.management.nfc.engine.EncodeMorseManager;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class NFCReadFragment extends DialogFragment {

    static final String TAG = NFCReadFragment.class.getSimpleName();
    private static final String HELLO = "Hello, welcome back";
    private static final String BYE = "Bye, see you soon.";
    private TextView mTvMessage;
    private Listener fragmentDisplayedListener;
    private StaffListener staffNameListener;
    private AppDatabase employeeListDb;

    static NFCReadFragment newInstance() {

        return new NFCReadFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_read_tag_event), "NFCReadFragment shown");
        View view = inflater.inflate(R.layout.fragment_read, container, false);
        employeeListDb = AppDatabase.getInstance(getActivity());
        initViews(view);
        return view;
    }

    private void initViews(View view) {

        mTvMessage = view.findViewById(R.id.tv_message);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof TagManagementActivity) {
            fragmentDisplayedListener = (TagManagementActivity) context;
        } else {
            fragmentDisplayedListener = (TagReaderActivity) context;
            staffNameListener = (TagReaderActivity) context;
        }

        fragmentDisplayedListener.onDialogDisplayed();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentDisplayedListener.onDialogDismissed();
    }

    void onNfcDetectedManager(Ndef ndef) {
        readFromNFCManager(ndef);
    }

    void onNfcDetectedStaff(Ndef ndef) {
        readFromNFCStaff(ndef);
    }

    String returnEmployerName(Ndef ndef) {
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
                message = EncodeMorseManager.getEncodedString(message);
                Log.d(TAG, "readFromNFCManager: " + message);
                String name = TimesheetUtil.getStaffName(message);
                String uId = TimesheetUtil.getStaffUniqueId(message);
                String employer = TimesheetUtil.getEmployer(message);
                AppExecutors.getInstance().diskIO().execute(() -> {
                    boolean availability;
                    if (employeeListDb.employeeDao().loadEmployeeByUid(uId) != null) {
                        availability = employeeListDb.employeeDao().loadEmployeeByUid(uId).isEmployeeAvailable();
                    } else {
                        availability = true;
                    }
                    if (getView() != null) {
                        String greeting = !availability ? HELLO : BYE;
                        GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_read_tag_event), uId + greeting);

                        Snackbar snackbar = Snackbar
                                .make(getView(), greeting, Snackbar.LENGTH_LONG);
                        View snackbarView = snackbar.getView();
                        snackbarView.setBackgroundColor(Color.WHITE);
                        TextView textView = snackbarView.findViewById(com.google.android.material.R.id.snackbar_text);
                        textView.setTextColor(!availability ? Color.DKGRAY : Color.MAGENTA);
                        textView.setGravity(Gravity.CENTER);
                        snackbar.show();
                    }

                });

                mTvMessage.setText(TimesheetUtil.parseNFCMessageStaff(message));
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_read_tag_event), uId + "-tag read success");
                staffNameListener.onStaffDetails(name, uId, employer);
            } else {
                mTvMessage.setText(R.string.empty_tag);
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_read_tag_event), this.getString(R.string.empty_tag));

            }
        } catch (IOException | FormatException e) {
            e.printStackTrace();

        }
    }

    private String getNdefMessage(Ndef ndef) throws IOException, FormatException {
        NdefMessage ndefMessage;
        String message = "";
        if (ndef != null) {
            ndef.connect();
            ndefMessage = ndef.getNdefMessage();
            if (ndefMessage != null) {
                message = new String(ndefMessage.getRecords()[0].getPayload());
            }
            ndef.close();
        }
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
                GAPAnalytics.sendEventGA(this.getClass().getSimpleName(), this.getString(R.string.analytics_read_tag_event), "getNdefMessage is empty");

            }
        } catch (IOException | FormatException e) {
            e.printStackTrace();
        }
    }
}
