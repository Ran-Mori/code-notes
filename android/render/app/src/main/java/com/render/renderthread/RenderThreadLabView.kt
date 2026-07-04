package com.render.renderthread

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

class RenderThreadLabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onEvent: (String) -> Unit = {}
) : View(context, attrs, defStyleAttr) {

    var autoInvalidate: Boolean = false
    var heavyDraw: Boolean = false
    var drawCount: Long = 0L
        private set

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(250, 252, 255)
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 226, 235)
        strokeWidth = dp(1f)
    }
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(31, 111, 235)
        strokeWidth = dp(3f)
        style = Paint.Style.STROKE
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(18, 161, 80)
    }
    private val heavyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(80, 231, 94, 73)
        strokeWidth = dp(1f)
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(37, 43, 52)
        textSize = sp(12f)
    }
    private val strongLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 25, 32)
        textSize = sp(13.5f)
        isFakeBoldText = true
    }
    private val rect = RectF()

    private var phase = 0.15f
    private var lastLoggedDraw = 0L

    init {
        setWillNotDraw(false)
    }

    fun advance(frameTimeNanos: Long) {
        phase = ((frameTimeNanos % 2_000_000_000L) / 2_000_000_000f)
        if (autoInvalidate) {
            postInvalidateOnAnimation()
        }
    }

    fun resetCounters() {
        drawCount = 0L
        lastLoggedDraw = 0L
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val threadName = Thread.currentThread().name
        val startNs = System.nanoTime()
        Trace.beginSection("RTLab.View.onDraw(record DisplayList)")
        try {
            drawCount++
            drawScene(canvas, threadName)
        } finally {
            Trace.endSection()
        }
        val durationMs = (System.nanoTime() - startNs) / 1_000_000.0
        if (drawCount == 1L || drawCount - lastLoggedDraw >= 45L) {
            lastLoggedDraw = drawCount
            val countSnapshot = drawCount
            val heavySnapshot = heavyDraw
            post {
                onEvent(
                    "View.onDraw #$countSnapshot on $threadName; " +
                        "recording took ${"%.2f".format(durationMs)}ms; heavy=$heavySnapshot"
                )
            }
        }
    }

    private fun drawScene(canvas: Canvas, threadName: String) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        drawGrid(canvas)

        val left = dp(24f)
        val right = width - dp(24f)
        val top = dp(88f)
        val bottom = height - dp(42f)
        rect.set(left, top, right, bottom)
        canvas.drawRoundRect(rect, dp(16f), dp(16f), pathPaint)

        val x = left + (right - left) * phase
        val y = ((top + bottom) / 2f) + sin(phase * PI.toFloat() * 2f) * dp(32f)
        canvas.drawCircle(x, y, dp(22f), circlePaint)

        if (heavyDraw) {
            drawHeavyWork(canvas, left, top, right, bottom)
        }

        canvas.drawText("View.onDraw thread = $threadName", dp(18f), dp(28f), strongLabelPaint)
        canvas.drawText("Canvas calls here record drawing commands for HWUI.", dp(18f), dp(50f), labelPaint)
        canvas.drawText("RenderThread consumes the recorded work after traversal.", dp(18f), dp(70f), labelPaint)
        canvas.drawText("drawCount=$drawCount  autoInvalidate=$autoInvalidate  heavyDraw=$heavyDraw", dp(18f), height - dp(16f), labelPaint)
    }

    private fun drawGrid(canvas: Canvas) {
        val step = dp(24f)
        var x = 0f
        while (x <= width) {
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
            x += step
        }
        var y = 0f
        while (y <= height) {
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
            y += step
        }
    }

    private fun drawHeavyWork(canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float) {
        var i = 0
        while (i < 950) {
            val t = i / 949f
            val x = left + (right - left) * t
            val y = top + (bottom - top) * ((sin((t + phase) * PI.toFloat() * 8f) + 1f) / 2f)
            canvas.drawLine(left, y, x, bottom - (y - top), heavyPaint)
            i++
        }
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity
}
