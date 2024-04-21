package com.binder.weixin;

import android.os.RemoteException;

public class CalculatorServer extends Stub {

    @Override
    public int add(int a, int b) throws RemoteException {
        return a + b;
    }
}
