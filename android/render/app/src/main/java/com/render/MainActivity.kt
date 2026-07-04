package com.render

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Trace
import android.util.Log
import android.view.Choreographer
import android.view.FrameMetrics
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        const val TAG = "RenderThreadLab"
        private const val SIXTY_HZ_NS = 16_666_666L
        private const val THIRTY_HZ_NS = 33_333_333L
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameStats = FrameStats()

    private lateinit var labView: RenderThreadLabView
    private lateinit var surfaceLabView: SurfaceCanvasLabView
    private lateinit var statsText: TextView
    private lateinit var eventText: TextView
    private lateinit var propertySquare: TextView

    private var metricsThread: HandlerThread? = null
    private var propertyAnimator: AnimatorSet? = null
    private var choreographerActive = false
    private var choreographerFrames = 0L
    private var lastChoreographerLogNs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            Trace.beginSection("RTLab.Choreographer#doFrame(main)")
            try {
                choreographerFrames++
                labView.advance(frameTimeNanos)
                if (frameTimeNanos - lastChoreographerLogNs > 1_000_000_000L) {
                    lastChoreographerLogNs = frameTimeNanos
                    appendEvent(
                        "Choreographer#doFrame on ${Thread.currentThread().name}; " +
                            "custom onDraw count=${labView.drawCount}"
                    )
                }
            } finally {
                Trace.endSection()
                if (choreographerActive) {
                    Choreographer.getInstance().postFrameCallback(this)
                }
            }
        }
    }

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
                statsText.text = snapshot.format()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Trace.beginSection("RTLab.Activity#onCreate")
        try {
            setContentView(createContentView())
            installFrameMetrics()
            appendEvent("App started on ${Thread.currentThread().name}; hardware acceleration is enabled.")
        } finally {
            Trace.endSection()
        }
    }

    override fun onResume() {
        super.onResume()
        choreographerActive = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
        surfaceLabView.resumeRenderer()
    }

    override fun onPause() {
        choreographerActive = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        propertyAnimator?.cancel()
        surfaceLabView.pauseRenderer()
        super.onPause()
    }

    override fun onDestroy() {
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

        content.addView(label("RenderThread Lab", 26f, Color.rgb(20, 25, 32), Typeface.BOLD))
        content.addView(
            label(
                "目标：用 App 层可观察的证据理解 RenderThread。你不能直接调用它；" +
                    "你能看到 UI 线程何时记录绘制命令、FrameMetrics 如何变化，以及 Perfetto 里 RenderThread 在哪里接手。",
                15f,
                Color.rgb(70, 77, 88),
                Typeface.NORMAL
            )
        )
        content.addView(spacer(10))
        content.addView(
            codeBlock(
                "VSync -> Choreographer#doFrame(main)\n" +
                    "      -> ViewRoot traversal / View.onDraw(main, record DisplayList)\n" +
                    "      -> RenderThread syncFrameState + DrawFrame\n" +
                    "      -> Skia + OpenGL/Vulkan + GPU\n" +
                    "      -> SurfaceFlinger composition"
            )
        )

        content.addView(sectionTitle("1. 普通 View 路径"))
        val scene = FrameLayout(this).apply {
            background = rounded(Color.WHITE, 16f, Color.rgb(225, 230, 238), 1)
            clipToPadding = false
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        labView = RenderThreadLabView(this, onEvent = ::appendEvent)
        scene.addView(
            labView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(245)
            )
        )
        propertySquare = TextView(this).apply {
            text = getString(R.string.render_node_transform)
            textSize = 11f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(Color.rgb(31, 111, 235), 12f, Color.TRANSPARENT, 0)
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }
        val squareParams = FrameLayout.LayoutParams(dp(82), dp(58)).apply {
            leftMargin = dp(22)
            topMargin = dp(168)
        }
        scene.addView(propertySquare, squareParams)
        content.addView(
            scene,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(262)
            ).apply { topMargin = dp(8) }
        )

        content.addView(sectionTitle("2. 实验开关"))
        content.addView(
            switchRow("每个 VSync 后 postInvalidateOnAnimation", false) { _, checked ->
                labView.autoInvalidate = checked
                appendEvent(if (checked) "Custom View starts invalidating every frame." else "Custom View stops invalidating.")
            }
        )
        content.addView(
            switchRow("让 onDraw 做更多 CPU 工作", false) { _, checked ->
                labView.heavyDraw = checked
                labView.invalidate()
                appendEvent(if (checked) "Heavy onDraw enabled: watch DRAW_DURATION and missed frames." else "Heavy onDraw disabled.")
            }
        )
        content.addView(
            switchRow("只做属性动画，不主动重绘 View", false) { button, checked ->
                startOrStopPropertyAnimation(checked)
                button.text = if (checked) "属性动画运行中：看 onDraw 是否同步增长" else "只做属性动画，不主动重绘 View"
            }
        )
        content.addView(buttonRow(
            button("阻塞主线程 700ms") { blockMainThread() },
            button("清空统计") { resetStats() }
        ))
        content.addView(button("写一条观察提示到 Logcat") {
            appendEvent("Use: adb logcat -s $TAG, then record Perfetto and look for main / RenderThread / SurfaceFlinger.")
        })

        content.addView(sectionTitle("3. FrameMetrics"))
        statsText = codeBlock(FrameStatsSnapshot.empty().format())
        content.addView(statsText)

        content.addView(sectionTitle("4. SurfaceView 对照组"))
        content.addView(
            label(
                "下面这个 SurfaceView 使用 App 自己创建的 AppSurfaceThread 画 Canvas。" +
                    "它不是 RenderThread；它是另一个 buffer producer，用来避免把“我自己起的绘制线程”和系统 HWUI RenderThread 混在一起。",
                14f,
                Color.rgb(70, 77, 88),
                Typeface.NORMAL
            )
        )
        surfaceLabView = SurfaceCanvasLabView(this, onEvent = ::appendEvent)
        content.addView(
            surfaceLabView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(156)
            ).apply { topMargin = dp(8) }
        )

        content.addView(sectionTitle("5. 事件日志"))
        eventText = codeBlock("Waiting for events...")
        content.addView(eventText)

        return root
    }

    private fun installFrameMetrics() {
        metricsThread = HandlerThread("FrameMetricsCollector").apply { start() }
        window.addOnFrameMetricsAvailableListener(
            metricsListener,
            Handler(metricsThread!!.looper)
        )
    }

    private fun startOrStopPropertyAnimation(start: Boolean) {
        propertyAnimator?.cancel()
        propertyAnimator = null
        if (!start) {
            propertySquare.translationX = 0f
            propertySquare.rotation = 0f
            propertySquare.alpha = 1f
            appendEvent("Property animation stopped.")
            return
        }
        propertySquare.post {
            val parentWidth = (propertySquare.parent as View).width
            val distance = (parentWidth - propertySquare.width - dp(54)).coerceAtLeast(dp(120)).toFloat()
            val move = ObjectAnimator.ofFloat(propertySquare, View.TRANSLATION_X, 0f, distance).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 1_700L
            }
            val rotate = ObjectAnimator.ofFloat(propertySquare, View.ROTATION, -4f, 8f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 1_700L
            }
            val fade = ObjectAnimator.ofFloat(propertySquare, View.ALPHA, 1f, 0.62f).apply {
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                duration = 1_700L
            }
            propertyAnimator = AnimatorSet().apply {
                playTogether(move, rotate, fade)
                start()
            }
            appendEvent("Property animation started. Compare visual motion with custom onDraw count.")
        }
    }

    private fun blockMainThread() {
        appendEvent("About to sleep main thread for 700ms. RenderThread cannot invent new View updates during this.")
        Trace.beginSection("RTLab.mainThreadSleep700ms")
        try {
            Thread.sleep(700L)
        } finally {
            Trace.endSection()
        }
        appendEvent("Main thread woke up.")
    }

    private fun resetStats() {
        synchronized(frameStats) { frameStats.reset() }
        labView.resetCounters()
        statsText.text = FrameStatsSnapshot.empty().format()
        appendEvent("Counters reset.")
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

    private fun label(text: String, sp: Float, color: Int, style: Int): TextView =
        TextView(this).apply {
            this.text = text
            textSize = sp
            setTextColor(color)
            typeface = Typeface.create(Typeface.DEFAULT, style)
            includeFontPadding = true
            setLineSpacing(0f, 1.12f)
        }

    private fun sectionTitle(text: String): TextView =
        label(text, 18f, Color.rgb(20, 25, 32), Typeface.BOLD).apply {
            setPadding(0, dp(18), 0, dp(4))
        }

    private fun codeBlock(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 12.5f
            setTextColor(Color.rgb(37, 43, 52))
            typeface = Typeface.MONOSPACE
            setPadding(dp(12), dp(10), dp(12), dp(10))
            background = rounded(Color.rgb(236, 240, 245), 12f, Color.rgb(220, 226, 234), 1)
            setLineSpacing(0f, 1.08f)
        }

    private fun switchRow(
        text: String,
        checked: Boolean,
        onChanged: (CompoundButton, Boolean) -> Unit
    ): Switch =
        Switch(this).apply {
            this.text = text
            textSize = 14.5f
            setTextColor(Color.rgb(45, 51, 62))
            isChecked = checked
            setPadding(0, dp(6), 0, dp(6))
            setOnCheckedChangeListener(onChanged)
        }

    private fun button(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 13f
            isAllCaps = false
            setOnClickListener { onClick() }
        }

    private fun buttonRow(vararg buttons: Button): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            for (button in buttons) {
                addView(
                    button,
                    LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                        marginEnd = dp(8)
                    }
                )
            }
        }

    private fun spacer(heightDp: Int): View =
        View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(heightDp)
            )
        }

    private fun rounded(fill: Int, radiusDp: Float, stroke: Int, strokeDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            if (strokeDp > 0) {
                setStroke(dp(strokeDp), stroke)
            }
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private class FrameStats {
        private var frames = 0L
        private var slow16 = 0L
        private var slow33 = 0L
        private var totalNs = 0L
        private var drawNs = 0L
        private var syncNs = 0L
        private var commandNs = 0L
        private var swapNs = 0L
        private var gpuNs = 0L
        private var gpuSamples = 0L
        private var lastTotalNs = 0L

        fun record(
            totalNs: Long,
            drawNs: Long,
            syncNs: Long,
            commandNs: Long,
            swapNs: Long,
            gpuNs: Long
        ): FrameStatsSnapshot {
            frames++
            if (totalNs > SIXTY_HZ_NS) slow16++
            if (totalNs > THIRTY_HZ_NS) slow33++
            this.totalNs += totalNs.coerceAtLeast(0L)
            this.drawNs += drawNs.coerceAtLeast(0L)
            this.syncNs += syncNs.coerceAtLeast(0L)
            this.commandNs += commandNs.coerceAtLeast(0L)
            this.swapNs += swapNs.coerceAtLeast(0L)
            if (gpuNs >= 0L) {
                this.gpuNs += gpuNs
                gpuSamples++
            }
            lastTotalNs = totalNs
            return snapshot()
        }

        fun reset() {
            frames = 0L
            slow16 = 0L
            slow33 = 0L
            totalNs = 0L
            drawNs = 0L
            syncNs = 0L
            commandNs = 0L
            swapNs = 0L
            gpuNs = 0L
            gpuSamples = 0L
            lastTotalNs = 0L
        }

        private fun snapshot(): FrameStatsSnapshot =
            FrameStatsSnapshot(
                frames = frames,
                slow16 = slow16,
                slow33 = slow33,
                avgTotalNs = average(totalNs, frames),
                avgDrawNs = average(drawNs, frames),
                avgSyncNs = average(syncNs, frames),
                avgCommandNs = average(commandNs, frames),
                avgSwapNs = average(swapNs, frames),
                avgGpuNs = if (gpuSamples > 0L) average(gpuNs, gpuSamples) else -1L,
                lastTotalNs = lastTotalNs
            )

        private fun average(total: Long, count: Long): Long =
            if (count == 0L) 0L else total / count
    }

    private data class FrameStatsSnapshot(
        val frames: Long,
        val slow16: Long,
        val slow33: Long,
        val avgTotalNs: Long,
        val avgDrawNs: Long,
        val avgSyncNs: Long,
        val avgCommandNs: Long,
        val avgSwapNs: Long,
        val avgGpuNs: Long,
        val lastTotalNs: Long
    ) {
        fun format(): String =
            "frames=$frames  >16.6ms=$slow16  >33.3ms=$slow33\n" +
                "avg total=${ms(avgTotalNs)}  last total=${ms(lastTotalNs)}\n" +
                "avg draw=${ms(avgDrawNs)}  sync=${ms(avgSyncNs)}  command=${ms(avgCommandNs)}\n" +
                "avg swap=${ms(avgSwapNs)}  gpu=${if (avgGpuNs >= 0L) ms(avgGpuNs) else "API31+"}\n" +
                "Interpretation: draw is mostly UI-thread recording work; sync/command/swap point at HWUI/RenderThread/GPU handoff."

        companion object {
            fun empty(): FrameStatsSnapshot =
                FrameStatsSnapshot(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, -1L, 0L)

            private fun ms(ns: Long): String =
                String.format(Locale.US, "%.2fms", ns / 1_000_000.0)
        }
    }
}
