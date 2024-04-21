package com.measurelayoutdraw.view

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import com.measurelayoutdraw.Single

class ButtonA(context: Context, attrs: AttributeSet?): androidx.appcompat.widget.AppCompatButton(context, attrs) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.e(Single.LOG_TAG, "ButtonA.onMeasure start, widthMeasureSpec=$widthMeasureSpec, heightMeasureSpec=$heightMeasureSpec")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.e(Single.LOG_TAG, "ButtonA.onMeasure end, widthMeasureSpec=$widthMeasureSpec, heightMeasureSpec=$heightMeasureSpec")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.e(Single.LOG_TAG, "ButtonA.onLayout start, changed=$changed, left=$left, top=$top, right=$right, bottom=$bottom")
        super.onLayout(changed, left, top, right, bottom)
        Log.e(Single.LOG_TAG, "ButtonA.onLayout end, changed=$changed, left=$left, top=$top, right=$right, bottom=$bottom")
    }

    override fun onDraw(canvas: Canvas?) {
        Log.e(Single.LOG_TAG, "ButtonA.onDraw start, canvas=$canvas")
        super.onDraw(canvas)
        Log.e(Single.LOG_TAG, "ButtonA.onDraw end, canvas=$canvas")
    }

}