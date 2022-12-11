package com.baj

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import androidx.appcompat.widget.AppCompatTextView

class SelfDefineTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): AppCompatTextView(context, attrs, defStyle) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        Log.d(MainActivity.TAG, "SelfDefineTextView onMeasure start")
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        Log.d(MainActivity.TAG, "SelfDefineTextView onMeasure end")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        Log.d(MainActivity.TAG, "SelfDefineTextView onLayout start")
        super.onLayout(changed, left, top, right, bottom)
        Log.d(MainActivity.TAG, "SelfDefineTextView onLayout end")
    }

    override fun onDraw(canvas: Canvas?) {
        Log.d(MainActivity.TAG, "SelfDefineTextView onDraw start")
        super.onDraw(canvas)
        Log.d(MainActivity.TAG, "SelfDefineTextView onDraw end")
    }
}