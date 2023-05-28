package com.binder.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class CalculatorClient {
    private ICalculator mCalculator;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mCalculator = ICalculator.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCalculator = null;
        }
    };

    public void connect(Context context) {
        Intent intent = new Intent(context, CalculatorService.class);
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect(Context context) {
        context.unbindService(mConnection);
    }

    public int add(int a, int b) throws RemoteException {
        return mCalculator.add(a, b);
    }

}
