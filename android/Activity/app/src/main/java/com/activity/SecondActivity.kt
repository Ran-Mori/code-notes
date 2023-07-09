package com.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("IzumiSakai", "SecondActivity onCreate")
        setContentView(R.layout.activity_second)
    }

    override fun onStart() {
        super.onStart()
        Log.d("IzumiSakai", "SecondActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("IzumiSakai", "SecondActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("IzumiSakai", "SecondActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("IzumiSakai", "SecondActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("IzumiSakai", "SecondActivity onDestroy")
    }
}