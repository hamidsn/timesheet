package com.tag.management.nfc;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import com.google.common.util.concurrent.ListenableFuture;
import com.tag.management.nfc.worker.MidnightDBCleanup;
import com.tag.management.nfc.worker.MidnightFinder;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

public class TimesheetUtil {
    public static final String WORKERTAG = "HAMID";
    public static final String WORKER_ONCE_TAG = "HAMIDONCE";
    public static final String EMPTY_EMPLOYER_UID = "EMPTY_EMPLOYER_UID";
    public static final String TIMESHEET_PREF = "TimesheetPref";
    public static final String WRONG_CHILD_NAME_FBDB = "WRONG_CHILD_NAME_FBDB";
    private static final String PATTERN_REGISTRATION = "MM/dd/yyyy";
    private static final String EMPLOYEE = "Employee: ";
    private static final String WRONG_TAG = "Tag read error";
    private static final String EMPLOYER = "Employer:";
    private static final String REGISTEREDAT = "Registered at ";
    private static final String TITLE = "Title";
    private static final String newLine = "\n";
    private static final String PATTERN_CURRENT = "EEE d MMM HH:mm";
    private static final String PATTERN_DATE = "EEE dd MMM yyyy";
    private static final String PATTERN_MONTH = "MM";
    private static final String PATTERN_DAY = "dd";
    private static final String PATTERN_YEAR = "yyyy";
    private static final String DASH_CHAR = "-";
    private static final String EMPLOYER_UID_INFO = "employer_uid_info";
    private static final String PREF_EMPLOYER_UID = "pref_employer_uid";
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
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_DATE);
        return dateFormat.format(date);
    }

    public static String getCurrentMonthUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_MONTH);
        return dateFormat.format(date);
    }

    public static String getCurrentDayUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_DAY);
        return dateFormat.format(date);
    }

    public static String getCurrentYearUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_YEAR);
        return dateFormat.format(date);
    }

    public static void applyDailyWorker(Context context) {

        if (!isWorkScheduled(WORKERTAG)) {

            PeriodicWorkRequest.Builder periodicWorkRequest =
                    new PeriodicWorkRequest.Builder(
                            MidnightDBCleanup.class,
                            1440, // mid night - 24 hours from now
                            TimeUnit.MINUTES)
                            .addTag(WORKERTAG)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 58, TimeUnit.MINUTES)
                            .setInputData(new Data.Builder()
                                    .putString(EMPLOYER_UID_INFO, getEmployerUid(context))
                                    .build());

            PeriodicWorkRequest myWork = periodicWorkRequest.build();
            WorkManager.getInstance().enqueue(myWork);
            Log.d("worker:", " PeriodicWorkRequest is running for every day- 1440 minutes");

        } else {
            Log.d("worker:", " periodic: A worker with HAMID tag is already scheduled");
        }
    }

    public static void applyOnceoffWorker() {
        if (!isWorkScheduled(WORKER_ONCE_TAG)) {
            WorkManager workerInstance = WorkManager.getInstance();
            //run once off workers
            OneTimeWorkRequest midnightWorkRequest =
                    new OneTimeWorkRequest.Builder(MidnightFinder.class)
                            .setInitialDelay(getMinutesTillMidnight(), TimeUnit.MINUTES)
                            .addTag(WORKER_ONCE_TAG)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, OneTimeWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                            //.setInitialDelay(17L, TimeUnit.MINUTES)
                            .build();
            Log.d("worker:", "running midnight finder with " + getMinutesTillMidnight() + " Minutes");
            try {
                workerInstance.enqueueUniqueWork(WORKER_ONCE_TAG, ExistingWorkPolicy.REPLACE, midnightWorkRequest);
            } catch (Exception e) {
                Log.d("worker:", "error" + e.getMessage());
            }
        } else {
            Log.d("worker:", " onceoff: A worker with HAMID tag is already scheduled");
        }
    }

    private static boolean isWorkScheduled(String tag) {
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

    private static long getMinutesTillMidnight() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return (c.getTimeInMillis() - System.currentTimeMillis()) / 60000;
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
            //after midnight
            absoluteMilis = System.currentTimeMillis() - today.getTimeInMillis();
        } else {
            //before midnight
            absoluteMilis = tomorrow.getTimeInMillis() - System.currentTimeMillis();
        }

        return absoluteMilis;
    }

    public static String getEmployerUid(Context context) {
        SharedPreferences pref = context.getSharedPreferences(TIMESHEET_PREF, 0);
        return employerUid.isEmpty() ? pref.getString(PREF_EMPLOYER_UID, EMPTY_EMPLOYER_UID) : employerUid;
    }

    static void setEmployerUid(String employerUid, Context context) {
        TimesheetUtil.employerUid = employerUid;
        SharedPreferences pref = context.getSharedPreferences(TIMESHEET_PREF, 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(PREF_EMPLOYER_UID, employerUid);
        editor.apply();
    }

    public static String getMonth(int month) {
        return new DateFormatSymbols().getMonths()[month - 1];
    }

    @NonNull
    public static String validateStringFB(String childName) {
        if (TextUtils.isEmpty(childName)) {
            childName = WRONG_CHILD_NAME_FBDB;
        }
        return childName.replace(".", DASH_CHAR).replace(" ", DASH_CHAR).replace("#", DASH_CHAR).replace("$", DASH_CHAR).replace("[", DASH_CHAR).replace("]", DASH_CHAR);
    }
}
