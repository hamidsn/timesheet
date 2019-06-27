package com.tag.management.nfc;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;

import androidx.annotation.NonNull;

public class GAPAnalytics {
    private static FirebaseAnalytics mFirebaseAnalytics;

    static FirebaseAnalytics instance(Context context, String uid) {
        if (mFirebaseAnalytics == null) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
            mFirebaseAnalytics.setUserProperty("user_id",uid.isEmpty() ? "empty_uid" : uid);
        }
        return mFirebaseAnalytics;
    }

    public static void sendEventGA(@NonNull String eventCategory, @NonNull String eventAction,
                                   @NonNull String eventLabel) {
        Bundle params = new Bundle();
        params.putString("eventCategory", eventCategory);
        params.putString("eventAction", eventAction);
        params.putString("eventLabel", eventLabel);
        if(mFirebaseAnalytics != null){
            mFirebaseAnalytics.logEvent("share_image", params);
        }

    }
}
