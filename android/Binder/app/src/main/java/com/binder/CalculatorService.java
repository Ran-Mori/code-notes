package com.binder;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class CalculatorService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private CalculatorImpl mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("IzumiSakai", "CalculatorService onCreate()");
        mBinder = new CalculatorImpl();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("IzumiSakai", "CalculatorService onDestroy()");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
