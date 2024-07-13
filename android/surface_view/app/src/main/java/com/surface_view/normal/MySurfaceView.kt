package com.surface_view.normal

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.fragment.app.FragmentActivity

class MySurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : SurfaceView(context, attrs, defStyle) {

    private val holderCallBack by lazy {
        object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                drawThread = DrawThread(holder)
                drawThread?.running = true
                drawThread?.start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                // Stop draw thread
                drawThread?.running = false
                drawThread = null
            }
        }
    }

    // Initialize drawing objects
    private var circleX: Float = 0f
    private val circleY: Float = ((context as? FragmentActivity)?.windowManager?.currentWindowMetrics?.bounds?.height()?.toFloat() ?: 0.0f) / 2
    private val circleRadius: Float = 200f
    private val circlePaint: Paint = Paint().apply { color = Color.RED }

    // Initialize surface holder and thread
    private var drawThread: DrawThread? = null

    init {
        // Add surface holder callback
        holder.addCallback(holderCallBack)
    }

    private inner class DrawThread(private val surfaceHolder: SurfaceHolder) : Thread() {

        var running = false

        override fun run() {
            while (running) {
                // Lock canvas and draw circle
                surfaceHolder.lockCanvas()?.let { canvas ->
                    canvas.drawColor(Color.WHITE)
                    canvas.drawCircle(circleX, circleY, circleRadius, circlePaint)

                    // Increment circle position and unlock canvas
                    circleX += 10f
                    surfaceHolder.unlockCanvasAndPost(canvas)
                }
                // Pause thread and reset circle position
                sleep(400)
                if (circleX > width + circleRadius) {
                    circleX = -circleRadius
                }
            }
        }
    }
}
