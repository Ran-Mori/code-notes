package com.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import android.util.TypedValue




class CanvasImageView : AppCompatImageView {

    companion object {
        fun dp2px(context: Context, dp: Int): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        val point = dp2px(context, 100)
        val radius = dp2px(context, 50)
        val paint = Paint().apply {
            color = resources.getColor(R.color.green, null)
            isAntiAlias = true
        }
        canvas?.drawCircle(point, point, radius, paint)
    }
}