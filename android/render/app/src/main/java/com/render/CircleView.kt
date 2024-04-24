package com.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View

class CircleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): View(context, attrs, defStyle) {

    companion object {
        fun dp2px(context: Context, dp: Int): Float =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics)
    }

    private var drawCircleFlag = false
    private val point = dp2px(context, 100)
    private val radius = dp2px(context, 50)
    private val paint =  Paint().apply {
        color = resources.getColor(R.color.teal_200, null)
        isAntiAlias = true
    }

    init {
        setOnClickListener {
            drawCircleFlag = !drawCircleFlag
            invalidate()
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (drawCircleFlag) {
            canvas?.drawARGB(255, 0, 0,0)
            canvas?.drawCircle(point, point, radius, paint)
        } else {
            canvas?.drawARGB(255, 255, 255,255)
        }
    }
}