package com.binder.weixin;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

// it runs in remote process
public class CalculatorService extends Service {

    private static final int NOTIFICATION_ID = 1;
    private CalculatorServer mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("IzumiSakai", "CalculatorService onCreate()");
        mBinder = new CalculatorServer();
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
