package com.tag.management.nfc;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tag.management.nfc.database.ReportEntry;
import com.tag.management.nfc.worker.MidnightDBCleanup;
import com.tag.management.nfc.worker.MidnightFinder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimesheetUtil {
    private static final String WORKERTAG = "HAMID";
    private static final String WORKER_ONCE_TAG = "HAMIDONCE";
    private static final String EMPTY_EMPLOYER_UID = "EMPTY_EMPLOYER_UID";
    private static final String TIMESHEET_PREF = "TimesheetPref";
    private static final String WRONG_CHILD_NAME_FBDB = "WRONG_CHILD_NAME_FBDB";
    private static final String PATTERN_REGISTRATION = "MM/dd/yyyy";
    private static final String EMPLOYEE = "Employee: ";
    private static final String WRONG_TAG = "Tag read error";
    private static final String EMPLOYER = "Employer:";
    private static final String REGISTEREDAT = "Registered at ";
    private static final String TITLE = "Title";
    private static final String newLine = "\n";
    private static final String PATTERN_CURRENT = "EEE. dd MMM. HH:mm";
    private static final String PATTERN_CONVERT = "EEE. dd MMM. HH:mm yyyy";
    private static final String PATTERN_MONTH = "MM";
    private static final String PATTERN_DAY = "dd";
    private static final String PATTERN_YEAR = "yyyy";
    private static final String EMPLOYER_UID_INFO = "employer_uid_info";
    private static final String PREF_EMPLOYER_UID = "pref_employer_uid";
    private static final String REGEX = "-";
    private static final String HASH = "#";
    private static final String DOLLAR = "$";
    private static final String OPEN_BRA = "[";
    private static final String CLOSE_BRA = "]";
    private static final String SPACE = " ";
    private static final String DOT = ".";
    public static boolean isDoing = false;
    private static String employerUid = "";
    private static Long totalMinutes = 0L;

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
        String[] items = message.split(newLine);
        return items.length > 0 ? items[1] : WRONG_TAG;
    }

    static String getStaffName(String message) {
        String[] items = message.split(newLine);
        return items[0];
    }

    static String getStaffUniqueId(String message) {
        String[] items = message.split(newLine);
        return items.length > 2 ? items[2] : WRONG_TAG;
    }

    private static String getActualTime(String s) {
        long timeStamp = Long.parseLong(s) * 1000L;
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_REGISTRATION, Locale.ENGLISH);
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }

    static String getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_CURRENT, Locale.ENGLISH);
        return dateFormat.format(date);
    }

    public static String getCurrentMonthUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_MONTH, Locale.ENGLISH);
        return dateFormat.format(date);
    }

    public static String getCurrentDayUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_DAY, Locale.ENGLISH);
        return dateFormat.format(date);
    }

    public static String getCurrentYearUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(PATTERN_YEAR, Locale.ENGLISH);
        return dateFormat.format(date);
    }

    public static void applyDailyWorker(Context context) {

        if (schedulingNewWork(WORKERTAG)) {

            PeriodicWorkRequest.Builder periodicWorkRequest =
                    new PeriodicWorkRequest.Builder(
                            MidnightDBCleanup.class,
                            1440, // mid night - 24 hours from now
                            TimeUnit.MINUTES)
                            .addTag(WORKERTAG)
                            .setBackoffCriteria(BackoffPolicy.LINEAR, 5, TimeUnit.MINUTES) // should be 60
                            .setInputData(new Data.Builder()
                                    .putString(EMPLOYER_UID_INFO, getEmployerUid(context))
                                    .build());

            PeriodicWorkRequest myWork = periodicWorkRequest.build();
            WorkManager.getInstance().enqueueUniquePeriodicWork(WORKERTAG, ExistingPeriodicWorkPolicy.KEEP, myWork); //enqueue(myWork);
            Log.d("worker:", " PeriodicWorkRequest is running for every day- 1440 minutes");

        } else {
            Log.d("worker:", " periodic: A worker with HAMID tag is already scheduled");
        }
    }

    public static void applyOnceoffWorker() {
        if (schedulingNewWork(WORKER_ONCE_TAG)) {
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

    private static boolean schedulingNewWork(String tag) {
        WorkManager instance = WorkManager.getInstance();
        ListenableFuture<List<WorkInfo>> statuses = instance.getWorkInfosByTag(tag);
        try {
            boolean running = false;
            List<WorkInfo> workInfoList = statuses.get();
            for (WorkInfo workInfo : workInfoList) {
                WorkInfo.State state = workInfo.getState();
                running = (state == WorkInfo.State.RUNNING | state == WorkInfo.State.ENQUEUED) | running;
            }
            return !running;
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            return true;
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

    @NonNull
    public static String validateStringFB(String childName) {
        if (TextUtils.isEmpty(childName)) {
            childName = WRONG_CHILD_NAME_FBDB;
        }
        return childName.replace(DOT, REGEX).replace(SPACE, REGEX).replace(HASH, REGEX).replace(DOLLAR, REGEX).replace(OPEN_BRA, REGEX).replace(CLOSE_BRA, REGEX);
    }

    static List<ReportEntry> filterStaffTimetable(List<ReportEntry> staff) {
        List<ReportEntry> mFinalList = new ArrayList<>();

        for (int i = 0; i < staff.size(); i++) {

            if (!TextUtils.isEmpty(staff.get(i).getEmployeeTimestampIn()) && staff.get(i).getEmployeeTimestampIn().contains(REGEX)) {
                String[] splittedTimestampIn = staff.get(i).getEmployeeTimestampIn().split(REGEX);
                String[] splittedTimestampOut = staff.get(i).getEmployeeTimestampOut().split(REGEX);
                for (int j = 0; j < splittedTimestampIn.length; j++) {
                    staff.add(new ReportEntry(staff.get(i).getEmployeeFullName(), splittedTimestampIn[j], splittedTimestampOut[j], staff.get(i).getEmployeeUniqueId(), staff.get(i).getEmployerName(), staff.get(i).getId()));
                }
            }
        }
        /////
        Collections.sort(staff, (obj1, obj2) -> obj1.getEmployeeFullName().compareToIgnoreCase(obj2.getEmployeeFullName()));
        /////

        for (ReportEntry person : staff) {
            if (!person.getEmployeeTimestampIn().contains(REGEX)) {
                mFinalList.add(person);
            }
        }
        return mFinalList;
    }

    static void createHTML(Context context, List<ReportEntry> mFinalList, String employerUid, String startDate, String endDate) {
        StringBuilder htmlDocument = new StringBuilder();
        htmlDocument.append(context.getString(R.string.template1).replace("$start", startDate).replace("$end", endDate));


        //Clean duplicated names() those with - in timing
        ArrayList<String> withoutDuplicatedNames = new ArrayList<>();
        for (ReportEntry element : mFinalList) {
            if (!withoutDuplicatedNames.contains(element.getEmployeeFullName())) {
                withoutDuplicatedNames.add(element.getEmployeeFullName());
            }
        }

        //create the html content
        for (int i = 0; i < withoutDuplicatedNames.size(); i++) {
            htmlDocument.append(context.getString(R.string.template2).replace("$name", withoutDuplicatedNames.get(i)).replace("$name", withoutDuplicatedNames.get(i)));

            StringBuilder htmlDocumentBuilder = new StringBuilder(htmlDocument.toString());
            for (int j = 0; j < mFinalList.size(); j++) {
                if (mFinalList.get(j).getEmployeeFullName().equals(withoutDuplicatedNames.get(i))) {
                    String timestampIn = mFinalList.get(j).getEmployeeTimestampIn();
                    String timestampOut = mFinalList.get(j).getEmployeeTimestampOut();
                    htmlDocumentBuilder.append(context.getString(R.string.template3).
                            replace("$timeIn", timestampIn).
                            replace("$timeOut", timestampOut).
                            replace("$Sum", calculateTimeBetween(timestampIn, timestampOut)));
                }
            }
            htmlDocument = new StringBuilder(htmlDocumentBuilder.toString());
            htmlDocument.append(context.getString(R.string.template4).replace("$totalHours", timeUnitToFullTime(totalMinutes, TimeUnit.MINUTES)));
            totalMinutes = 0L;
        }
        htmlDocument.append(context.getString(R.string.template5));
        saveHTMLFile(context, htmlDocument.toString(), employerUid);

        Log.d("html", "" + htmlDocument);
    }

    private static String calculateTimeBetween(String timestampIn, String timestampOut) {
        Timestamp in = convertStringToTimestamp(timestampIn);
        Timestamp out = convertStringToTimestamp(timestampOut);
        boolean validTiming = (in != null && out != null);

        totalMinutes += validTiming ? ((Math.abs(in.getTime() - out.getTime())) / 1000 / 60) : 0;
        return validTiming
                ? timeUnitToFullTime(Math.abs(in.getTime() - out.getTime()), TimeUnit.MILLISECONDS)
                : " - ";
    }

    @SuppressLint("DefaultLocale")
    private static String timeUnitToFullTime(long time, TimeUnit timeUnit) {
        long hour = timeUnit.toHours(time);
        long minute = timeUnit.toMinutes(time) % 60;
        String result = "0";
        if (hour > 0) {
            result = String.format("%d(h):%02d(m)", hour, minute);
        } else if (minute > 0) {
            result = String.format("%d(m)", minute);
        }
        return result;
    }

    //this is used to create report
    @SuppressLint("SimpleDateFormat")
    private static Timestamp convertStringToTimestamp(String str_date) {
        java.sql.Timestamp timeStampDate;
        try {
            DateFormat formatter;
            formatter = new SimpleDateFormat(PATTERN_CURRENT, Locale.ENGLISH);
            Date date = formatter.parse(str_date);
            timeStampDate = new Timestamp(date.getTime());
        } catch (ParseException e) {
            Log.d("ParseException", "Exception :" + e);
            timeStampDate = null;
        }
        return timeStampDate;
    }

    @SuppressLint("SimpleDateFormat")
    static Timestamp convertStringToTimestamp(String str_date, String year) throws ParseException {
        java.sql.Timestamp timeStampDate;
        Date date;
        DateFormat formatter;
        formatter = new SimpleDateFormat(PATTERN_CONVERT, Locale.ENGLISH);
        try {
            date = formatter.parse(str_date.split(REGEX)[0] + SPACE + year);
            timeStampDate = new Timestamp(date.getTime());
        } catch (ParseException e) {
            //when "." is missing is month in FB
            Log.d("ParseException", "Exception :" + e);
            //date = new SimpleDateFormat(PATTERN_CONVERT, Locale.ENGLISH).parse(str_date.split(REGEX)[0].substring(0, 11)+ ". " + str_date.split(REGEX)[0].substring(12, str_date.split(REGEX)[0].length()) + SPACE + year);
            date = new SimpleDateFormat(PATTERN_CONVERT, Locale.ENGLISH).parse(String.format("%s. %s%s%s", str_date.split(REGEX)[0].substring(0, 11), str_date.split(REGEX)[0].substring(12), SPACE, year));
            timeStampDate = new Timestamp(date.getTime());
        }
        return timeStampDate;
    }

    private static void saveHTMLFile(Context context, String data, String employerUid) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("timesheet.html", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
        // send file to firebase
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("timesheets/reports/" + employerUid + "/timesheet.html");
        Uri file = Uri.fromFile(context.getFileStreamPath("timesheet.html"));
        UploadTask uploadTask = storageRef.putFile(file);
        uploadTask.continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw Objects.requireNonNull(task.getException());
            }

            // Continue with the task to get the download URL
            return storageRef.getDownloadUrl();
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Uri downloadUrl = task.getResult();
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(downloadUrl);
                context.startActivity(i);
            } else {
                //todo Handle failures
                // ...
            }
        });
    }

    static boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (connectivityManager != null) {
            activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    static boolean isEncodable(String input) {
        String specialChars = " @/='()-_!?:;,.ßÜÖÄ";
        char currentCharacter;
        boolean isEncodable = true;

        for (int i = 0; i < input.length(); i++) {
            currentCharacter = input.charAt(i);

            if (!(Character.isDigit(currentCharacter)
                    || Character.isUpperCase(currentCharacter)
                    || Character.isLowerCase(currentCharacter)
                    || specialChars.contains(String.valueOf(currentCharacter)))) {

                isEncodable = false;
            }
        }
        return isEncodable;
    }
}
