package com.render.bitmapgl

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Trace
import android.util.Log
import android.view.FrameMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.render.R
import com.render.common.FrameStats
import com.render.common.FrameStatsSnapshot
import com.render.common.button
import com.render.common.buttonRow
import com.render.common.codeBlock
import com.render.common.dp
import com.render.common.label
import com.render.common.rounded
import com.render.common.sectionTitle
import com.render.common.spacer

class BitmapOpenGlLabActivity : Activity() {

    companion object {
        const val TAG = "BitmapOpenGlLab"
        private const val FRAME_NOTE =
            "Interpretation: GL producer timing is logged separately; FrameMetrics describes the Activity window composition."
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameStats = FrameStats()

    private lateinit var bitmapOpenGlLabView: BitmapOpenGlLabView
    private lateinit var statusText: TextView
    private lateinit var statsText: TextView
    private lateinit var eventText: TextView
    private var metricsThread: HandlerThread? = null

    private val metricsListener = Window.OnFrameMetricsAvailableListener { _, metrics, _ ->
        val gpuNs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            metrics.getMetric(FrameMetrics.GPU_DURATION)
        } else {
            -1L
        }
        val snapshot = synchronized(frameStats) {
            frameStats.record(
                totalNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION),
                drawNs = metrics.getMetric(FrameMetrics.DRAW_DURATION),
                syncNs = metrics.getMetric(FrameMetrics.SYNC_DURATION),
                commandNs = metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION),
                swapNs = metrics.getMetric(FrameMetrics.SWAP_BUFFERS_DURATION),
                gpuNs = gpuNs
            )
        }
        if (snapshot.frames % 12L == 0L) {
            mainHandler.post {
                statsText.text = snapshot.format(FRAME_NOTE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Trace.beginSection("RTLab.BitmapOpenGlActivity#onCreate")
        try {
            setContentView(createContentView())
            installFrameMetrics()
            appendEvent("Bitmap/OpenGL/TextureView page created on ${Thread.currentThread().name}.")
        } finally {
            Trace.endSection()
        }
    }

    override fun onResume() {
        super.onResume()
        bitmapOpenGlLabView.onHostResume()
    }

    override fun onPause() {
        bitmapOpenGlLabView.onHostPause()
        super.onPause()
    }

    override fun onDestroy() {
        bitmapOpenGlLabView.release()
        window.removeOnFrameMetricsAvailableListener(metricsListener)
        metricsThread?.quitSafely()
        super.onDestroy()
    }

    private fun createContentView(): View {
        val root = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(247, 248, 250))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(28))
        }
        root.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(label("Bitmap → OpenGL → TextureView", 25f, Color.rgb(20, 25, 32), Typeface.BOLD))
        content.addView(
            label(
                "目标：在独立 GL 线程用 BitmapFactory 解码 JPG，显式上传为 OpenGL 2D 纹理，" +
                    "再把 shader 绘制结果通过 EGL window surface 投递给 TextureView。",
                15f,
                Color.rgb(70, 77, 88),
                Typeface.NORMAL
            )
        )
        content.addView(spacer(10))
        content.addView(
            codeBlock(
                "drawable-nodpi/bitmap_gl_sample.jpg\n" +
                    "  -> BitmapFactory.decodeResource (CPU Bitmap)\n" +
                    "  -> GLUtils.texImage2D (OpenGL texture)\n" +
                    "  -> vertex shader + fragment shader + glDrawArrays\n" +
                    "  -> EGLSurface(Surface(TextureView.surfaceTexture))\n" +
                    "  -> eglSwapBuffers -> TextureView -> HWUI -> app window"
            )
        )

        content.addView(sectionTitle("1. OpenGL 输出画面"))
        val scene = FrameLayout(this).apply {
            background = rounded(Color.WHITE, 16f, Color.rgb(225, 230, 238), 1)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        bitmapOpenGlLabView = BitmapOpenGlLabView(
            this,
            onEvent = ::appendEvent,
            onStatus = ::updateStatus
        )
        scene.addView(
            bitmapOpenGlLabView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(250)
            )
        )
        scene.addView(
            TextView(this).apply {
                text = getString(R.string.bitmap_gl_normal_view_overlay)
                textSize = 11.5f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = rounded(Color.argb(215, 145, 55, 230), 999f, Color.TRANSPARENT, 0)
            },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(16)
                rightMargin = dp(16)
            }
        )
        content.addView(
            scene,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(266)
            ).apply { topMargin = dp(8) }
        )

        content.addView(sectionTitle("2. 对照实验"))
        content.addView(
            buttonRow(
                button("重解码 JPG + 重传") { bitmapOpenGlLabView.decodeUploadAndDraw() },
                button("复用 GL 纹理重绘") { bitmapOpenGlLabView.drawExistingTexture() }
            )
        )
        content.addView(
            label(
                "前者重复 CPU 解码与 texImage2D；后者跳过这两步，只复用 textureId 绘制并 swap。对比事件日志里的耗时。",
                13.5f,
                Color.rgb(82, 89, 100),
                Typeface.NORMAL
            )
        )

        content.addView(sectionTitle("3. GL / TextureView 状态"))
        statusText = codeBlock(bitmapOpenGlLabView.currentStatus())
        content.addView(statusText)

        content.addView(sectionTitle("4. Activity FrameMetrics"))
        statsText = codeBlock(FrameStatsSnapshot.empty().format(FRAME_NOTE))
        content.addView(statsText)

        content.addView(sectionTitle("5. 事件日志"))
        eventText = codeBlock("Waiting for events...")
        content.addView(eventText)

        return root
    }

    private fun installFrameMetrics() {
        metricsThread = HandlerThread("BitmapGlFrameMetricsCollector").apply { start() }
        window.addOnFrameMetricsAvailableListener(
            metricsListener,
            Handler(metricsThread!!.looper)
        )
    }

    private fun updateStatus(status: String) {
        if (!this::statusText.isInitialized) return
        statusText.text = status
    }

    @SuppressLint("SetTextI18n")
    private fun appendEvent(message: String) {
        Log.d(TAG, message)
        if (!this::eventText.isInitialized) return
        mainHandler.post {
            val current = eventText.text.toString()
            val lines = if (current == "Waiting for events...") {
                emptyList()
            } else {
                current.lines()
            }
            eventText.text = (lines + message).takeLast(14).joinToString("\n")
        }
    }
}
