package com.binder.weixin;

public interface IRemoteCalculator  {
    int add(int a, int b) throws android.os.RemoteException;
}
