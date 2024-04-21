package com.selfdefineview.view

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.selfdefineview.R

//直接继承自View
class CircleView: View {

    companion object {
        private const val DEFAULT_PX_SIZE = 800
    }

    constructor(context: Context): this(context, null)

    constructor(context: Context, attrs: AttributeSet?): this(context, attrs, 0)

    @SuppressLint("Recycle")
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int): super(context, attrs, defStyleAttr) {
        //自定义属性
        val a: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.CircleView)
        val mColor = a.getColor(R.styleable.CircleView_circle_color, Color.RED)
        mPaint.color = mColor
    }

    private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val widthSpecMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSpecMode = MeasureSpec.getMode(heightMeasureSpec)
        val widthSpecSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSpecSize = MeasureSpec.getSize(heightMeasureSpec)

        //支持wrap_parent
        if (widthSpecMode == MeasureSpec.AT_MOST && heightSpecMode == MeasureSpec.AT_MOST) {
            //wrap parent默认300px
            setMeasuredDimension(DEFAULT_PX_SIZE, DEFAULT_PX_SIZE)
        } else if (widthSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(DEFAULT_PX_SIZE, heightSpecSize)
        } else if (heightSpecMode == MeasureSpec.AT_MOST) {
            setMeasuredDimension(widthSpecSize, DEFAULT_PX_SIZE)
        } else {
            setMeasuredDimension(widthSpecSize, heightSpecSize)
        }
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        //support for padding
        val width = width - paddingLeft - paddingEnd
        val height = height - paddingTop - paddingBottom

        val radius = (width.coerceAtMost(height) / 2).toFloat()
        val x = ((paddingLeft + width) / 2).toFloat()
        val y = ((paddingTop + height) / 2).toFloat()
        canvas?.drawCircle(x, y, radius, mPaint)
    }
}