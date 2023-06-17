package com.cbbootreceiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MyBootReceiver: BroadcastReceiver() {

    private var mContext: Context? = null


    override fun onReceive(context: Context?, intent: Intent?) {
        mContext = context
        if (intent?.action?.equals(Intent.ACTION_BOOT_COMPLETED) == true) {
            Log.d("IzumiSakai", "receive Intent.ACTION_BOOT_COMPLETED")
        }
    }
}