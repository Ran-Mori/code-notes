package com.baj

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View

class DoubleClickListener(
    private val view: View,
    private val onDoubleClick: ((view: View?) -> Unit)?
): View.OnTouchListener {
    private val gestureDetector =
        GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent?): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent?): Boolean {
                return true
            }

            override fun onDoubleTap(e: MotionEvent?): Boolean {
                onDoubleClick?.invoke(view)
                return true
            }
        })

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}