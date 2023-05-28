package com.binder.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class CalculatorService extends Service {

    private CalculatorImpl mBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBinder = new CalculatorImpl();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}
