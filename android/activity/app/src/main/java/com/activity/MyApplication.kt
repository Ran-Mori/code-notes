package com.activity

import android.app.Application
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi

class MyApplication: Application() {
    @RequiresApi(33)
    override fun onCreate() {
        super.onCreate()
        // 由于指定了android:process，因此第二个Activity启动时会新建一个Application
        Log.d("IzumiSakai", "${this.hashCode()} MyApplication onCreate, process = ${Process.myProcessName()}")
    }
}