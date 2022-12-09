package com.baj

import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

class DoubleClickListener(
    private val view: View,
    private val onDoubleClickListener: View.OnClickListener?,
    private val onSingleClickListener: View.OnClickListener?
): View.OnTouchListener {

    init {
        Log.d(MainActivity.TAG, "DoubleClickListener mDoubleClickInterval = ${ViewConfiguration.getLongPressTimeout().toLong()}")
    }

    private val mSingleClickTask = Runnable { onSingleClickListener?.onClick(view) }

    private val mDoubleClickInterval = ViewConfiguration.getDoubleTapTimeout().toLong()

    private val gestureDetector =
        GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {

            override fun onDown(e: MotionEvent): Boolean {
                return true
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                view.postDelayed(mSingleClickTask, mDoubleClickInterval)
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                view.removeCallbacks(mSingleClickTask)
                onDoubleClickListener?.onClick(view)
                return true
            }
        })

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }
}