package com.example.dispatch.intercept

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class InnerInterceptViewGroup@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): FrameLayout(context, attrs, defStyle) {

    private var needIntercept: Boolean = false

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return when (ev?.action) {
            MotionEvent.ACTION_DOWN -> false
            MotionEvent.ACTION_MOVE -> needIntercept
            MotionEvent.ACTION_UP -> false
            else -> false
        }
    }
}