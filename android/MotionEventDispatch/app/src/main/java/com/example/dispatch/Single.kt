package com.example.dispatch

import android.view.MotionEvent

object Single {
    const val LOG_TAG = "IzumiSakai"

    fun Int?.getMotionEventString(): String =
        when(this) {
            MotionEvent.ACTION_DOWN -> "DOWN"
            MotionEvent.ACTION_UP -> "UP"
            MotionEvent.ACTION_MOVE -> "MOVE"
            MotionEvent.ACTION_CANCEL -> "CANCEL"
            MotionEvent.ACTION_OUTSIDE -> "OUTSIDE"
            MotionEvent.ACTION_POINTER_DOWN -> "POINTER_DOWN"
            MotionEvent.ACTION_POINTER_UP -> "POINTER_UP"
            MotionEvent.ACTION_SCROLL -> "SCROLL"
            else -> "other"
        }
}