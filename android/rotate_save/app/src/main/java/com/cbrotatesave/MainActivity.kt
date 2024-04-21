package com.cbrotatesave

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val message = savedInstanceState?.getString("IzumiSakai")
        if (message == null) {
            Log.d("IzumiSakai","message == null")
        } else {
            Log.d("IzumiSakai", "message = $message")
        }
    }

    //只有在意外退出时才会调用此方法，用户强意愿的主动退出不会调用此方法
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("IzumiSakai", "this is a data from onSaveInstanceState")
        super.onSaveInstanceState(outState)
    }

}