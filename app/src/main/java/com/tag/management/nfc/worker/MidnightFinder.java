package com.tag.management.nfc.worker;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import com.tag.management.nfc.GAPAnalytics;
import com.tag.management.nfc.TimesheetUtil;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MidnightFinder extends Worker {
    private final Context mContext;

    public MidnightFinder(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        // midnight DB clean up
        GAPAnalytics.sendEventGA("MidnightFinder", "MidnightFinder error", "midnight finder worker is running");

        try {
            TimesheetUtil.applyDailyWorker(this.mContext);

            return Result.success();
        } catch (Exception e) {
            GAPAnalytics.sendEventGA("MidnightFinder", "MidnightFinder error", "error - onceoff worker - " + e.getMessage());

            return Result.retry();
        }
    }

}
