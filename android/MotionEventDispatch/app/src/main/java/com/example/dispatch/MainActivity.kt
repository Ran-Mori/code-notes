package com.example.dispatch

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import com.example.dispatch.Single.getMotionEventString

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "MainActivity.dispatchTouchEvent call start, action: ${ev?.action?.getMotionEventString()}")
        val result = super.dispatchTouchEvent(ev)
        Log.e(Single.LOG_TAG, "MainActivity.dispatchTouchEvent call end, result: $result, action: ${ev?.action?.getMotionEventString()}")
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "MainActivity.onTouchEvent call start, action: ${event?.action?.getMotionEventString()}")
        val result = super.onTouchEvent(event)
        Log.e(Single.LOG_TAG, "MainActivity.onTouchEvent call end, result: $result, action: ${event?.action?.getMotionEventString()}")
        return result
    }
}