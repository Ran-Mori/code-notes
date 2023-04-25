package com.baj

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout

class SelfDefineFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): FrameLayout(context, attrs, defStyle) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(MainActivity.TAG, "SelfDefineFrameLayout onMeasure start")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(MainActivity.TAG, "SelfDefineFrameLayout onMeasure end")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(MainActivity.TAG, "SelfDefineFrameLayout onLayout start")
        super.onLayout(changed, left, top, right, bottom)
        Log.d(MainActivity.TAG, "SelfDefineFrameLayout onLayout end")
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d(MainActivity.TAG, "SelfDefineFrameLayout onDraw start")
        super.onDraw(canvas)
        Log.d(MainActivity.TAG, "SelfDefineFrameLayout onDraw end")
    }
}