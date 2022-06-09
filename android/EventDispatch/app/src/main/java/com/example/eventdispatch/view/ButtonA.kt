package com.example.eventdispatch.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.example.eventdispatch.Single

class ButtonA(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatButton(context, attrs) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnTouchListener { _, event ->
            Log.e(Single.LOG_TAG, "ConstraintLayoutA.OnTouchListener call, action: ${event?.action}")
            //consume this event?
            return@setOnTouchListener true
//            return@setOnTouchListener false
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ButtonA.dispatchTouchEvent call start, action: ${event?.action}")
        val result = super.dispatchTouchEvent(event)
        Log.e(Single.LOG_TAG, "ButtonA.dispatchTouchEvent call end, result: $result, action: ${event?.action}")
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ButtonA.onTouchEvent call start, action: ${event?.action}")
        val result = super.onTouchEvent(event)
        Log.e(Single.LOG_TAG, "ButtonA.onTouchEvent call end, result: $result, action: ${event?.action}")
        return result
    }


}