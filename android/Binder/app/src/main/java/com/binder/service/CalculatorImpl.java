package com.binder.service;

import android.os.RemoteException;

public class CalculatorImpl extends ICalculator.Stub {
    @Override
    public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString) throws RemoteException {
        // do nothing
    }

    @Override
    public int add(int a, int b) throws RemoteException {
        return a + b;
    }
}
