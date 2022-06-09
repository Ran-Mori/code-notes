package com.measurelayoutdraw.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.measurelayoutdraw.Single

class ConstraintLayoutB(context: Context, attrs: AttributeSet?): ConstraintLayout(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onMeasure start, widthMeasureSpec=$widthMeasureSpec, heightMeasureSpec=$heightMeasureSpec")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onMeasure end, widthMeasureSpec=$widthMeasureSpec, heightMeasureSpec=$heightMeasureSpec")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onLayout start, changed=$changed, left=$left, top=$top, right=$right, bottom=$bottom")
        super.onLayout(changed, left, top, right, bottom)
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onLayout end, changed=$changed, left=$left, top=$top, right=$right, bottom=$bottom")
    }

    override fun onDraw(canvas: Canvas?) {
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onDraw start, canvas=$canvas")
        super.onDraw(canvas)
        Log.e(Single.LOG_TAG, "ConstraintLayoutB.onDraw end, canvas=$canvas")
    }

}