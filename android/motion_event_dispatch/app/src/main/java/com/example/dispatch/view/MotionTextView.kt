package com.example.dispatch.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatTextView
import com.example.dispatch.Single
import com.example.dispatch.Single.getMotionEventString

class MotionTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): AppCompatTextView(context, attrs, defStyle) {
    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_UP) {
            Log.d(Single.DISPATCH_LOG_TAG, "ready to debug")
        }
        return super.dispatchTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Log.e(Single.DISPATCH_LOG_TAG, "MotionTextView.onTouchEvent call start, action: ${event?.action?.getMotionEventString()}")
        val result = super.onTouchEvent(event)
        Log.e(Single.DISPATCH_LOG_TAG, "MotionTextView.onTouchEvent call end, action: ${event?.action?.getMotionEventString()}, result = ${result}")
        return result
    }
}