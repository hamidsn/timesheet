package com.tag.management.nfc.worker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.tag.management.nfc.TimesheetUtil;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class MidnightFinder extends Worker {
    public MidnightFinder(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        // midnight DB clean up
        Log.d("worker", "midnight finder worker is running");

        TimesheetUtil.applyDailyWorker();

        return Result.success();
    }

}