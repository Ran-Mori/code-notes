package com.binder.douyin;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;


public class CalculatorClient {
    private ILocalCalculator mCalculator;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("IzumiSakai", "server onServiceConnected call");
            mCalculator = new Proxy(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mCalculator = null;
        }
    };

    public void connect(Context context) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.binder","com.binder.weixin.CalculatorService"));
        context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void disconnect(Context context) {
        context.unbindService(mConnection);
    }

    public int add(int a, int b) throws RemoteException {
        return mCalculator.add(a, b);
    }
}
