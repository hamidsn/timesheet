package com.tag.management.nfc;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;

import com.tag.management.nfc.worker.MidnightDBCleanup;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

public class TimesheetUtil {
    private static final String PATTERN_REGISTRATION = "MM/dd/yyyy";
    private static final String EMPLOYEE = "Employee: ";
    private static final String WRONG_TAG = "Tag read error";
    private static final String EMPLOYER = "Employer:";
    private static final String REGISTEREDAT = "Registered at ";
    private static final String TITLE = "Title";
    private static final String newLine = "\n";
    private static final String PATTERN_CURRENT = "EEE d MMM HH:mm";
    private static final String PATTERN_DAY = "EEE dd MMM yyyy";
    private static final String WORKERTAG = "worker_tag";
    public static boolean isDoing = false;
    public static String employerUid = "";

    static boolean isEmailValid(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    static Uri imageToUri(Bitmap bitmap, ContentResolver contentResolver) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, TITLE, null);
        return Uri.parse(path);
    }

    static String generateTimeStamp() {
        return String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
    }

    static String parseNFCMessageManager(String message) {
        String tagMessage;
        String[] items = message.split(newLine);
        if (items.length == 3) {
            tagMessage = EMPLOYEE + newLine + items[0] + newLine + newLine + EMPLOYER + newLine + items[1] + newLine + newLine + REGISTEREDAT + getActualTime(items[2]);

        } else {
            tagMessage = "not a valid tag for this app. It contains :" + newLine + items[0];
        }
        return tagMessage;
    }

    static String parseNFCMessageStaff(String message) {
        String tagMessage;
        String[] items = message.split(newLine);

        if (items.length == 3) {
            /*todo if sign in or sign out*/
            tagMessage = newLine + items[0] + newLine + newLine + getCurrentTimeUsingCalendar();

        } else {
            tagMessage = "not a valid tag for this app. It contains :" + newLine + items[0];
        }
        return tagMessage;
    }

    static String getEmployer(String message) {
        //String tagMessage;
        String[] items = message.split(newLine);
        return items.length > 0 ? items[1] : WRONG_TAG;
    }

    static String getStaffName(String message) {
        String[] items = message.split(newLine);
        return items[0];
    }

    static String getStaffUniqueId(String message) {
        String[] items = message.split(newLine);
        return items.length > 0 ? items[2] : WRONG_TAG;
    }

    private static String getActualTime(String s) {
        long timeStamp = Long.parseLong(s) * 1000L;
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_REGISTRATION);
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }

    public static String getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_CURRENT);
        return dateFormat.format(date);
    }

    public static String getCurrentDateUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_DAY);
        return dateFormat.format(date);
    }

    public static void applyDailyWorker(Context context) {

        WorkManager.getInstance().cancelAllWorkByTag(WORKERTAG);
        Log.d("worker", " DB cleaning worker is NOT running");

        PeriodicWorkRequest.Builder periodicWorkRequest =
                new PeriodicWorkRequest.Builder(
                        MidnightDBCleanup.class,
                        1430, // 23:50
                        TimeUnit.MINUTES)
                        .addTag(WORKERTAG)
                        .setInputData(new Data.Builder()
                                .putString("employer_uid", TimesheetUtil.getEmployerUid(context))
                                .build());

        PeriodicWorkRequest myWork = periodicWorkRequest.build();
        WorkManager.getInstance().enqueueUniquePeriodicWork(WORKERTAG, ExistingPeriodicWorkPolicy.KEEP, myWork);
    }


    public static long getMillisTillMidnight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis() - System.currentTimeMillis();
    }

    public static long getAbsoluteMillisTillMidnight() {
        Calendar tomorrow = Calendar.getInstance();
        tomorrow.add(Calendar.DATE, 1);
        tomorrow.set(Calendar.HOUR_OF_DAY, 0);
        tomorrow.set(Calendar.MINUTE, 0);
        tomorrow.set(Calendar.SECOND, 0);
        tomorrow.set(Calendar.MILLISECOND, 0);

        Calendar today = Calendar.getInstance();
        today.add(Calendar.DATE, 0);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        long absoluteMilis;

        if (System.currentTimeMillis() - today.getTimeInMillis() < 3600000) {
            absoluteMilis = System.currentTimeMillis() - today.getTimeInMillis();
        } else {
            absoluteMilis = tomorrow.getTimeInMillis() - System.currentTimeMillis();
        }

        return absoluteMilis;
    }

    public static String getEmployerUid(Context context) {
        SharedPreferences pref = context.getSharedPreferences("TimesheetPref", 0);
        return employerUid == null ? pref.getString("key_name", null) : employerUid;
    }

    static void setEmployerUid(String employerUid, Context context) {
        TimesheetUtil.employerUid = employerUid;
        SharedPreferences pref = context.getSharedPreferences("TimesheetPref", 0); // 0 - for private mode
        @SuppressLint("CommitPrefEdits") SharedPreferences.Editor editor = pref.edit();
        editor.putString("employer_uid", employerUid);
    }
}
