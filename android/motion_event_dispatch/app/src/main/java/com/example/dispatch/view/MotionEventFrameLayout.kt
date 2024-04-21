package com.example.dispatch.view

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.widget.FrameLayout
import com.example.dispatch.Single

class MotionEventFrameLayout@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): FrameLayout(context, attrs, defStyle) {

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_UP) {
            Log.d(Single.DISPATCH_LOG_TAG, "intercept up event")
            return true // 单独拦截up事件
        }
        return super.onInterceptTouchEvent(ev)
    }
}