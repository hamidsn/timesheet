package com.tag.management.nfc;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.nabinbhandari.android.permissions.PermissionHandler;
import com.nabinbhandari.android.permissions.Permissions;
import com.tag.management.nfc.engine.DecodeMorseManager;
import com.tag.management.nfc.model.Employee;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.fabric.sdk.android.Fabric;

// https://github.com/nabinbhandari/Android-Permissions


/**
 * {
 * "rules": {
 * <p>
 * // only authenticated users can read and write the messages node
 * ".read": "root.child(auth.uid).exists() && auth != null",
 * ".write": "auth != null"
 * <p>
 * <p>
 * }
 * }
 */

public class TagManagementActivity extends AppCompatActivity implements Listener {

    public static final String TAG = TagManagementActivity.class.getSimpleName();
    public static final int RC_SIGN_IN = 1;
    public static final String ANONYMOUS = "anonymous";
    public static final String READ_FROM_NFC = "readFromNfc";
    public static final String DATA = "data";
    public static final String EMPLOYEES = "employees";
    public static final String STAFF_PHOTOS = "staff_photos";
    public static final String INIT_NFC = "initNFC";
    public static final String AUTHENTICATION = "authentication";
    public static final String SIGNOUT = "signout";
    public static final String WRITE_2_NFC = "write2nfc";
    public static final String UTF_8 = "UTF-8";
    private String STARTUP_TRACE_NAME = "nfc_management_trace";

    private String mCurrentPhotoPath;
    private Uri photoURI;
    private EditText mEtName, mEtEmail;
    private ImageView photoPickerImage;
    private Button mBtWrite, mBtRead, mBImage;
    private String employerUid;
    private NFCWriteFragment mNfcWriteFragment;
    private NFCReadFragment mNfcReadFragment;

    private static final int RC_PHOTO_PICKER = 2;

    private boolean isDialogDisplayed = false;
    private boolean isWrite = false;
    private Uri downloadUrl = null;

    private NfcAdapter mNfcAdapter;

    private String employerName;
    //firebase instance variables
    private FirebaseAuth mFirebaseAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    //firebase storage
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mStaffPhotosStorageReference;

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mMessagesDatabaseReference;
    private ChildEventListener mChildEventListener;
    private Trace myTrace;
    private int PIC_CROP = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        initViews();

        initNFC();

        initFirebase();

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                // Sign-in succeeded, set up the UI
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                // Sign in was canceled by the user, finish the activity
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            //setPic();
            if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get(DATA);
                photoPickerImage.setImageBitmap(imageBitmap);
                photoURI = TimesheetUtil.imageToUri(imageBitmap, getApplicationContext().getContentResolver());
                storeImageFirebase();
            }
        }
    }

    private void storeImageFirebase() {
        StorageReference photoRef = mStaffPhotosStorageReference.child(photoURI.getLastPathSegment());
        // Upload file to Firebase Storage
        /*photoRef.putFile(photoURI)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // When the image has successfully uploaded, we get its download URL
                        downloadUrl = taskSnapshot.getDownloadUrl();
                        // Set the download URL to the message box, so that the user can send it to the database
                    }
                });*/

        //todo test download url
        UploadTask uploadTask =  photoRef.putFile(photoURI);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return photoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    downloadUrl = task.getResult();
                } else {
                    // Handle failures
                    // ...
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    private void initViews() {
        mEtName = (EditText) findViewById(R.id.et_name);
        mEtEmail = (EditText) findViewById(R.id.et_email);
        mBtWrite = (Button) findViewById(R.id.btn_write);
        mBtRead = (Button) findViewById(R.id.btn_read);
        mBImage = (Button) findViewById(R.id.btn_image);
        photoPickerImage = (ImageView) findViewById(R.id.photoPicker);

        mEtName.addTextChangedListener(inputValidation);
        mEtEmail.addTextChangedListener(inputValidation);
        mBtWrite.setEnabled(false);

        mBtWrite.setOnClickListener(view -> showWriteFragment());
        mBtRead.setOnClickListener(view -> showReadFragment());
        mBImage.setOnClickListener(view -> captureImage());
    }

    private void initFirebase() {
        Fabric.with(this, new Crashlytics());
        mFirebaseAuth = FirebaseAuth.getInstance();
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;

        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(EMPLOYEES);
        myTrace = FirebasePerformance.getInstance().newTrace(STARTUP_TRACE_NAME);
        myTrace.start();

        mFirebaseStorage = FirebaseStorage.getInstance();
        mStaffPhotosStorageReference = mFirebaseStorage.getReference().child(STAFF_PHOTOS);

        myTrace.incrementCounter(INIT_NFC);

        myTrace.incrementCounter(AUTHENTICATION);
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

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
            }
        };
    }

    private void captureImage() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {

                Permissions.check(this/*context*/, Manifest.permission.WRITE_EXTERNAL_STORAGE, null, new PermissionHandler() {
                    @Override
                    public void onGranted() {
                        startActivityForResult(takePictureIntent, RC_PHOTO_PICKER);
                    }

                    @Override
                    public void onDenied(Context context, ArrayList<String> deniedPermissions) {
                        // permission denied, block the feature.
                        Toast.makeText(TagManagementActivity.this, "You need to grant a permission to attach picture", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        }
    }

    private void initNFC() {
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }


    private void showWriteFragment() {
        isWrite = true;
        mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);
        if (mNfcWriteFragment == null) {
            mNfcWriteFragment = NFCWriteFragment.newInstance();
        }
        mNfcWriteFragment.show(getFragmentManager(), NFCWriteFragment.TAG);
    }

    private void showReadFragment() {
        mNfcReadFragment = (NFCReadFragment) getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
        if (mNfcReadFragment == null) {
            mNfcReadFragment = NFCReadFragment.newInstance();
        }
        mNfcReadFragment.show(getFragmentManager(), NFCReadFragment.TAG);
    }

    @Override
    public void onDialogDisplayed() {
        isDialogDisplayed = true;
    }

    @Override
    public void onDialogDismissed() {
        isDialogDisplayed = false;
        isWrite = false;
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
        detachDatabaseReadListener();

        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        myTrace.stop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                myTrace.incrementCounter(SIGNOUT);
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void onSignedInInitialize(String username, String uid) {
        employerName = username;
        employerUid = uid;
        mMessagesDatabaseReference = mFirebaseDatabase.getReference().child(employerUid);
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        employerName = ANONYMOUS;
        employerUid = ANONYMOUS;
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
//todo : solve me, always reads DB
        mChildEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Employee employee = dataSnapshot.getValue(Employee.class);
                if (mNfcWriteFragment != null) {
                    mNfcWriteFragment.dismiss();
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
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
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        Log.d(TAG, "onNewIntent: " + intent.getAction());

        if (tag != null) {
            Toast.makeText(this, getString(R.string.message_tag_detected), Toast.LENGTH_SHORT).show();

            if (isDialogDisplayed) {

                if (isWrite) {

                    String messageToWrite = mEtName.getText().toString();
                    String emailAddress = mEtEmail.getText().toString();
                    String employeeTimeStamp = TimesheetUtil.generateTimeStamp();

                    NdefRecord ndefRecord = createTextRecord(messageToWrite, employerName, employeeTimeStamp);
                    NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{ndefRecord});

                    myTrace.incrementCounter(WRITE_2_NFC);
                    mNfcWriteFragment = (NFCWriteFragment) getFragmentManager().findFragmentByTag(NFCWriteFragment.TAG);


                    if (mNfcWriteFragment.onNfcDetected(tag, ndefMessage, emailAddress, messageToWrite, employerName)) {
                        // write was successful, add employee to fb DB
                        Employee employee = new Employee(employerName, messageToWrite, employerUid, (Uri.EMPTY.equals(downloadUrl) || downloadUrl == null) ? "-" : downloadUrl.toString(), emailAddress, employeeTimeStamp);
                        mMessagesDatabaseReference.push().setValue(employee);
                    }

                } else {

                    myTrace.incrementCounter(READ_FROM_NFC);
                    mNfcReadFragment = (NFCReadFragment) getFragmentManager().findFragmentByTag(NFCReadFragment.TAG);
                    Ndef ndef = Ndef.get(tag);
                    mNfcReadFragment.onNfcDetectedManager(ndef);
                }
            }
        }
    }

    private NdefRecord createTextRecord(String staffName, String employerName, String employeeTimeStamp) {
        try {
            staffName = DecodeMorseManager.getDecodedString(staffName + "\n" + employerName + "\n" + employeeTimeStamp);
            final byte[] text = staffName.getBytes(UTF_8);
            final int textLength = text.length;
            final ByteArrayOutputStream payload = new ByteArrayOutputStream(/*1 + ? */  textLength);
            payload.write(text, 0, textLength);


            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], payload.toByteArray());
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "createTextRecord error: " + e.getMessage());
        }
        return null;
    }

    private TextWatcher inputValidation = new TextWatcher(){

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            mBtWrite.setEnabled(mEtName.getText().toString().length() > 0 && TimesheetUtil.isEmailValid(mEtEmail.getText().toString()));
        }

        @Override
        public void afterTextChanged(Editable s) {

        }

    };
}
