package com.cbapplicationsingleton

import android.app.Application
import android.util.Log

/**
 * 继承Application,并且修改AndroidManifest.xml中application标签的android:name属性就能自定义全局唯一的Application
 */
class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("IzumiSakai", "MyApplication onCreate")
    }
}