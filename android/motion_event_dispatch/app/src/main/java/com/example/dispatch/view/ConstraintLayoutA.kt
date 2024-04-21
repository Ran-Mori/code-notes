package com.example.dispatch.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.dispatch.Single
import com.example.dispatch.Single.getMotionEventString

class ConstraintLayoutA(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnTouchListener { _, event ->
            Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.OnTouchListener call start, action: ${event?.action?.getMotionEventString()}")

            //consume this event?
            val result = false
//            val result = true
            Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.OnTouchListener call end, result = ${result}, action: ${event?.action?.getMotionEventString()}")

            return@setOnTouchListener result
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.onInterceptTouchEvent call start, action: ${ev?.action?.getMotionEventString()}")
        val result = super.onInterceptTouchEvent(ev)
        Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.onInterceptTouchEvent call end, result: $result, action: ${ev?.action?.getMotionEventString()}")
        return result
    }


    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.dispatchTouchEvent call start, action: ${ev?.action?.getMotionEventString()}")
        val result = super.dispatchTouchEvent(ev)
        Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.dispatchTouchEvent call end, result: $result, action: ${ev?.action?.getMotionEventString()}")
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.onTouchEvent call start, action: ${event?.action?.getMotionEventString()}")
        val result = super.onTouchEvent(event)
        Log.e(Single.DISPATCH_LOG_TAG, "ConstraintLayoutA.onTouchEvent call end, result: $result, action: ${event?.action?.getMotionEventString()}")
        return result
    }
}