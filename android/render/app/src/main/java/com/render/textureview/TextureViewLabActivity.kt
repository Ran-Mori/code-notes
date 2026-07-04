package com.render.textureview

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.widget.Switch
import android.widget.TextView
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
import com.render.common.switchRow

class TextureViewLabActivity : Activity() {

    companion object {
        const val TAG = "TextureViewLab"
        private const val FRAME_NOTE =
            "Interpretation: producer frames come from TextureViewProducer; visible updates still pass through normal View composition."
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameStats = FrameStats()

    private lateinit var textureViewLabView: TextureViewLabView
    private lateinit var producerSwitch: Switch
    private lateinit var transformSwitch: Switch
    private lateinit var statsText: TextView
    private lateinit var statusText: TextView
    private lateinit var eventText: TextView

    private var metricsThread: HandlerThread? = null
    private var transformAnimator: AnimatorSet? = null

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
        Trace.beginSection("RTLab.TextureViewActivity#onCreate")
        try {
            setContentView(createContentView())
            installFrameMetrics()
            appendEvent("TextureView page started on ${Thread.currentThread().name}; hardware acceleration is enabled.")
        } finally {
            Trace.endSection()
        }
    }

    override fun onPause() {
        producerSwitch.isChecked = false
        transformSwitch.isChecked = false
        stopTransformAnimation()
        textureViewLabView.stopContinuousProducer(logEvent = false)
        super.onPause()
    }

    override fun onDestroy() {
        textureViewLabView.releaseProducer()
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

        content.addView(label("TextureView 学习", 26f, Color.rgb(20, 25, 32), Typeface.BOLD))
        content.addView(
            label(
                "目标：把你会用的 TextureView 拆成 View、SurfaceTexture、Surface、BufferQueue 和 producer/consumer 几个角色。" +
                    "它不是第二个 UI 线程，而是把外部 buffer 流接进普通 View 树。",
                15f,
                Color.rgb(70, 77, 88),
                Typeface.NORMAL
            )
        )
        content.addView(spacer(10))
        content.addView(
            codeBlock(
                "TextureView attached to window\n" +
                    "  -> SurfaceTexture becomes available\n" +
                    "  -> app creates Surface(surfaceTexture) for one producer\n" +
                    "  -> producer queues buffers by unlockCanvasAndPost / camera / video / GL\n" +
                    "  -> TextureView consumes newest buffer as a GLES texture\n" +
                    "  -> HWUI composites it with the normal View hierarchy"
            )
        )

        content.addView(sectionTitle("1. TextureView 画面"))
        val scene = FrameLayout(this).apply {
            background = rounded(Color.WHITE, 16f, Color.rgb(225, 230, 238), 1)
            clipToPadding = false
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        textureViewLabView = TextureViewLabView(
            this,
            onEvent = ::appendEvent,
            onStatus = ::updateStatus
        )
        scene.addView(
            textureViewLabView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(250)
            )
        )
        scene.addView(
            TextView(this).apply {
                text = "normal View overlay"
                textSize = 11.5f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(10), dp(6), dp(10), dp(6))
                background = rounded(Color.argb(205, 31, 111, 235), 999f, Color.TRANSPARENT, 0)
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

        content.addView(sectionTitle("2. 实验控制"))
        content.addView(
            buttonRow(
                button("producer 画一帧") { textureViewLabView.drawSingleFrame() },
                button("阻塞主线程 700ms") { blockMainThread() }
            )
        )
        producerSwitch = switchRow("后台 producer 连续往 Surface 投递 buffer", false) { button, checked ->
            if (checked) {
                textureViewLabView.startContinuousProducer()
                button.text = "producer 运行中：每 16ms lockCanvas/unlockCanvasAndPost"
            } else {
                textureViewLabView.stopContinuousProducer()
                button.text = "后台 producer 连续往 Surface 投递 buffer"
            }
        }
        content.addView(producerSwitch)
        transformSwitch = switchRow("对 TextureView 做旋转/透明度 View 动画", false) { button, checked ->
            if (checked) {
                startTransformAnimation()
                button.text = "View 动画运行中：buffer 内容和 View 变换分开看"
            } else {
                stopTransformAnimation()
                button.text = "对 TextureView 做旋转/透明度 View 动画"
            }
        }
        content.addView(transformSwitch)
        content.addView(button("清空统计") { resetStats() })

        content.addView(sectionTitle("3. TextureView 状态"))
        statusText = codeBlock(textureViewLabView.currentStatus())
        content.addView(statusText)

        content.addView(sectionTitle("4. FrameMetrics"))
        statsText = codeBlock(FrameStatsSnapshot.empty().format(FRAME_NOTE))
        content.addView(statsText)

        content.addView(sectionTitle("5. 事件日志"))
        eventText = codeBlock("Waiting for events...")
        content.addView(eventText)

        return root
    }

    private fun installFrameMetrics() {
        metricsThread = HandlerThread("TextureViewFrameMetricsCollector").apply { start() }
        window.addOnFrameMetricsAvailableListener(
            metricsListener,
            Handler(metricsThread!!.looper)
        )
    }

    private fun startTransformAnimation() {
        transformAnimator?.cancel()
        textureViewLabView.pivotX = textureViewLabView.width / 2f
        textureViewLabView.pivotY = textureViewLabView.height / 2f
        val rotate = ObjectAnimator.ofFloat(textureViewLabView, View.ROTATION, -2.5f, 2.5f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1_600L
        }
        val scaleX = ObjectAnimator.ofFloat(textureViewLabView, View.SCALE_X, 0.96f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1_600L
        }
        val scaleY = ObjectAnimator.ofFloat(textureViewLabView, View.SCALE_Y, 0.96f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1_600L
        }
        val alpha = ObjectAnimator.ofFloat(textureViewLabView, View.ALPHA, 0.72f, 1f).apply {
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            duration = 1_600L
        }
        transformAnimator = AnimatorSet().apply {
            playTogether(rotate, scaleX, scaleY, alpha)
            start()
        }
        appendEvent("TextureView View-property animation started; producer buffer content is unchanged.")
    }

    private fun stopTransformAnimation() {
        transformAnimator?.cancel()
        transformAnimator = null
        if (this::textureViewLabView.isInitialized) {
            textureViewLabView.rotation = 0f
            textureViewLabView.scaleX = 1f
            textureViewLabView.scaleY = 1f
            textureViewLabView.alpha = 1f
        }
    }

    private fun blockMainThread() {
        appendEvent("About to sleep main thread for 700ms. Producer may keep posting, but TextureView composition needs UI/HWUI progress.")
        Trace.beginSection("RTLab.TextureView.mainThreadSleep700ms")
        try {
            Thread.sleep(700L)
        } finally {
            Trace.endSection()
        }
        appendEvent("Main thread woke up.")
    }

    private fun resetStats() {
        synchronized(frameStats) { frameStats.reset() }
        textureViewLabView.resetCounters()
        statsText.text = FrameStatsSnapshot.empty().format(FRAME_NOTE)
        statusText.text = textureViewLabView.currentStatus()
        appendEvent("FrameMetrics reset.")
    }

    private fun updateStatus(status: String) {
        if (!this::statusText.isInitialized) return
        statusText.text = status
    }

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
            val next = (lines + message).takeLast(12).joinToString("\n")
            eventText.text = next
        }
    }
}
