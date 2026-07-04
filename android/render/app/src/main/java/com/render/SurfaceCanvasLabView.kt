package com.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.PI
import kotlin.math.sin

class SurfaceCanvasLabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onEvent: (String) -> Unit = {}
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private val running = AtomicBoolean(false)
    private var rendererThread: Thread? = null

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 25, 32)
    }
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 190, 75)
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(13f)
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startRenderer()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopRenderer()
    }

    fun resumeRenderer() {
        if (holder.surface.isValid) {
            startRenderer()
        }
    }

    fun pauseRenderer() {
        stopRenderer()
    }

    private fun startRenderer() {
        if (running.getAndSet(true)) return
        rendererThread = Thread(::renderLoop, "AppSurfaceThread").apply {
            isDaemon = true
            start()
        }
        post { onEvent("SurfaceView renderer started on AppSurfaceThread. This is not Android RenderThread.") }
    }

    private fun stopRenderer() {
        if (!running.getAndSet(false)) return
        val thread = rendererThread
        rendererThread = null
        if (thread != null && thread != Thread.currentThread()) {
            thread.join(300L)
        }
        post { onEvent("SurfaceView renderer stopped.") }
    }

    private fun renderLoop() {
        var frame = 0L
        while (running.get()) {
            val canvas = try {
                holder.lockCanvas()
            } catch (ignored: IllegalArgumentException) {
                null
            }
            if (canvas != null) {
                try {
                    drawFrame(canvas, frame)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            if (frame % 60L == 0L) {
                val producerThread = Thread.currentThread().name
                val frameSnapshot = frame
                post {
                    onEvent("SurfaceView frame #$frameSnapshot drawn by $producerThread; producer thread is AppSurfaceThread.")
                }
            }
            frame++
            Thread.sleep(16L)
        }
    }

    private fun drawFrame(canvas: Canvas, frame: Long) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        val phase = (frame % 120L) / 120f
        val x = dp(34f) + (width - dp(68f)) * phase
        val y = height / 2f + sin(phase * PI.toFloat() * 2f) * dp(24f)
        canvas.drawCircle(x, y, dp(18f), circlePaint)
        canvas.drawText("SurfaceView Canvas producer: AppSurfaceThread", dp(16f), dp(28f), textPaint)
        canvas.drawText("Separate app thread, separate Surface buffer producer.", dp(16f), dp(52f), textPaint)
    }

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity
}
