package com.tag.management.nfc;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Patterns;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimesheetUtil {
    private static final String PATTERN_REGISTRATION = "MM/dd/yyyy";
    private static final String EMPLOYEE = "Employee: ";
    private static final String HELLO = "Hello";
    private static final String BYE = "Bye";
    private static final String EMPLOYER = "Employer:";
    private static final String REGISTEREDAT = "Registered at ";
    private static final String TITLE = "Title";
    private static final String newLine = "\n";
    private static final String PATTERN_CURRENT = "EEE d MMM HH:mm";

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

    public static String parseNFCMessageManager(String message) {
        String tagMessage;
        String[] items = message.toString().split(newLine);
        if(items.length == 3) {
            tagMessage = EMPLOYEE + newLine + items[0] + newLine + newLine + EMPLOYER + newLine + items[1] + newLine + newLine + REGISTEREDAT + getActualTime(items[2]);

        } else {
            tagMessage = "not a valid tag for this app. It contains :" + newLine + items[0];
        }
        return tagMessage;
    }

    public static String parseNFCMessageStaff(String message) {
        String tagMessage;
        String[] items = message.toString().split(newLine);

        if(items.length == 3) {
            /*todo if sign in or sign out*/
            tagMessage = (true ? HELLO : BYE) + newLine + items[1] + newLine + newLine + getCurrentTimeUsingCalendar();

        } else {
            tagMessage = "not a valid tag for this app. It contains :" + newLine + items[0];
        }
        return tagMessage;
    }

    public static String getEmployer(String message) {
        String tagMessage;
        String[] items = message.toString().split(newLine);
        return items[1];
    }

    private static String getActualTime(String s) {
        long timeStamp = Long.parseLong(s) * 1000L;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(PATTERN_REGISTRATION);
            Date netDate = (new Date(timeStamp));
            return sdf.format(netDate);
        } catch (Exception ex) {
            return "xx";
        }
    }

    private static String getCurrentTimeUsingCalendar() {
        Calendar cal = Calendar.getInstance();
        Date date = cal.getTime();
        DateFormat dateFormat = new SimpleDateFormat(PATTERN_CURRENT);
        String formattedDate = dateFormat.format(date);
        return formattedDate;
    }
}
