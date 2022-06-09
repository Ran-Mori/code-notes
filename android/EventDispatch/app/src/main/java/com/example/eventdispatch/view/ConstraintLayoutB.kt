package com.example.eventdispatch.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.eventdispatch.Single

class ConstraintLayoutB(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnTouchListener { _, event ->
            Log.e(Single.LOG_TAG, "ConstraintLayoutB.OnTouchListener call, action: ${event?.action}")

            //consume this event?
            return@setOnTouchListener true
//            return@setOnTouchListener false
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onInterceptTouchEvent call start, action: ${ev?.action}")
        val result = super.onInterceptTouchEvent(ev)
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onInterceptTouchEvent call end, result: $result, action: ${ev?.action}")
        return result
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.dispatchTouchEvent call start, action: ${ev?.action}")
        val result = super.dispatchTouchEvent(ev)
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.dispatchTouchEvent call end, result: $result, action: ${ev?.action}")
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onTouchEvent call start, action: ${event?.action}")
        val result = super.onTouchEvent(event)
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onTouchEvent call end, result: $result, action: ${event?.action}")
        return result
    }
}