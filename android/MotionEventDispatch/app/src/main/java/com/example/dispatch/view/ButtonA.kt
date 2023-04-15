package com.example.dispatch.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.example.dispatch.Single
import com.example.dispatch.Single.getMotionEventString

class ButtonA(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatButton(context, attrs) {

    @SuppressLint("ClickableViewAccessibility")
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setOnTouchListener { _, event ->
            Log.e(Single.LOG_TAG, "ButtonA.OnTouchListener call start, action: ${event?.action?.getMotionEventString()}")

            //consume this event?
            val result = false
//            val result = true
            Log.e(Single.LOG_TAG, "ButtonA.OnTouchListener call end, result = ${result}, action: ${event?.action?.getMotionEventString()}")

            return@setOnTouchListener result
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ButtonA.dispatchTouchEvent call start, action: ${event?.action?.getMotionEventString()}")
        val result = super.dispatchTouchEvent(event)
        Log.e(Single.LOG_TAG, "ButtonA.dispatchTouchEvent call end, result: $result, action: ${event?.action?.getMotionEventString()}")
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.LOG_TAG, "ButtonA.onTouchEvent call start, action: ${event?.action?.getMotionEventString()}")
        val result = false // 强制不消费，让MainActivity#onTouchEvent()被调用
//        val result = super.onTouchEvent(event)
        Log.e(Single.LOG_TAG, "ButtonA.onTouchEvent call end, result: $result, action: ${event?.action?.getMotionEventString()}")
        return result
    }
}