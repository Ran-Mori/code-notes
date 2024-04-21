package com.example.dispatch.intercept

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout

class OuterInterceptChildView@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): View(context, attrs, defStyle) {

    private var needIntercept: Boolean = false

    override fun dispatchTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            // 当down给child时，置为true让parent先没有机会调用onInterceptTouchEvent()
            // 能否调用onInterceptTouchEvent()完全取决于下面的move事件是否放行
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_MOVE -> {
                if (needIntercept) {
                    // 该处理滑动冲突时，让parent下面的时候有机会拦截
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            // up能传到这里，说明上面的move从来没有让拦截过，正常处理
            MotionEvent.ACTION_UP -> {}
        }
        return super.dispatchTouchEvent(event)
    }
}