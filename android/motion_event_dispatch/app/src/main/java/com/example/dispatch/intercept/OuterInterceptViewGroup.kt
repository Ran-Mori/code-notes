package com.example.dispatch.intercept

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class OuterInterceptViewGroup@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): FrameLayout(context, attrs, defStyle) {
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        // down不拦截，其他事件是否拦截听child通知
        return ev?.action != MotionEvent.ACTION_DOWN
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // 在这里来解决滑动冲突
        return super.onTouchEvent(event)
    }
}