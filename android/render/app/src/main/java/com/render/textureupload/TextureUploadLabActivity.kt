package com.render.textureupload

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
import android.view.Choreographer
import android.view.FrameMetrics
import android.view.View
import android.view.ViewGroup
import android.view.Window
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
import com.render.common.sectionTitle
import com.render.common.spacer
import com.render.common.switchRow

class TextureUploadLabActivity : Activity() {

    companion object {
        const val TAG = "TextureUploadLab"
        private const val FRAME_NOTE =
            "Interpretation: main records drawBitmap; sync/command/swap/gpu are where HWUI texture upload pressure may surface."
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameStats = FrameStats()

    private lateinit var textureUploadLabView: TextureUploadLabView
    private lateinit var continuousTextureSwitch: Switch
    private lateinit var statsText: TextView
    private lateinit var eventText: TextView

    private var metricsThread: HandlerThread? = null
    private var choreographerActive = false
    private var choreographerFrames = 0L
    private var lastChoreographerLogNs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            Trace.beginSection("RTLab.Texture.Choreographer#doFrame(main)")
            try {
                choreographerFrames++
                textureUploadLabView.advance(frameTimeNanos)
                if (frameTimeNanos - lastChoreographerLogNs > 1_000_000_000L) {
                    lastChoreographerLogNs = frameTimeNanos
                    appendEvent(
                        "Texture page doFrame on ${Thread.currentThread().name}; " +
                            "texture onDraw count=${textureUploadLabView.drawCount}"
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
                statsText.text = snapshot.format(FRAME_NOTE)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Trace.beginSection("RTLab.TextureUploadActivity#onCreate")
        try {
            setContentView(createContentView())
            installFrameMetrics()
            appendEvent("Texture upload page started on ${Thread.currentThread().name}; hardware acceleration is enabled.")
        } finally {
            Trace.endSection()
        }
    }

    override fun onResume() {
        super.onResume()
        choreographerActive = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onPause() {
        choreographerActive = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onPause()
    }

    override fun onDestroy() {
        if (this::textureUploadLabView.isInitialized) {
            textureUploadLabView.releaseTextures(logEvent = false)
        }
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

        content.addView(label("大纹理上传", 26f, Color.rgb(20, 25, 32), Typeface.BOLD))
        content.addView(
            label(
                "目标：模拟业务里频繁刷新大图导致 Bitmap 内容变脏，下一帧 drawBitmap 后 HWUI 需要重新上传纹理到 GPU 的卡顿。",
                15f,
                Color.rgb(70, 77, 88),
                Typeface.NORMAL
            )
        )
        content.addView(spacer(10))
        content.addView(
            codeBlock(
                "main: mark Bitmap dirty with setPixel(1px)\n" +
                    "main: View.onDraw records drawBitmap\n" +
                    "RenderThread/HWUI: consume DisplayList and upload texture\n" +
                    "GPU/SurfaceFlinger: execute and present frame"
            )
        )

        content.addView(sectionTitle("1. 实验画布"))
        textureUploadLabView = TextureUploadLabView(this, onEvent = ::appendEvent)
        content.addView(
            textureUploadLabView,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(230)
            ).apply { topMargin = dp(8) }
        )

        content.addView(sectionTitle("2. 实验控制"))
        content.addView(
            buttonRow(
                button("准备大纹理") { textureUploadLabView.prepareTextures() },
                button("单次触发上传") { textureUploadLabView.requestSingleUpload() }
            )
        )
        continuousTextureSwitch = switchRow("连续每帧标记大纹理 dirty", false) { button, checked ->
            if (checked && !textureUploadLabView.prepareTexturesIfNeeded()) {
                button.isChecked = false
                return@switchRow
            }
            textureUploadLabView.continuousDirtyUpload = checked
            button.text = if (checked) {
                "连续上传中：每帧只改 1px，然后 drawBitmap"
            } else {
                "连续每帧标记大纹理 dirty"
            }
            appendEvent(if (checked) "Continuous dirty texture upload enabled." else "Continuous dirty texture upload disabled.")
        }
        content.addView(continuousTextureSwitch)
        content.addView(
            buttonRow(
                button("释放纹理") {
                    continuousTextureSwitch.isChecked = false
                    textureUploadLabView.releaseTextures()
                },
                button("清空统计") { resetStats() }
            )
        )

        content.addView(sectionTitle("3. FrameMetrics"))
        statsText = codeBlock(FrameStatsSnapshot.empty().format(FRAME_NOTE))
        content.addView(statsText)

        content.addView(sectionTitle("4. 事件日志"))
        eventText = codeBlock("Waiting for events...")
        content.addView(eventText)

        return root
    }

    private fun installFrameMetrics() {
        metricsThread = HandlerThread("TextureFrameMetricsCollector").apply { start() }
        window.addOnFrameMetricsAvailableListener(
            metricsListener,
            Handler(metricsThread!!.looper)
        )
    }

    private fun resetStats() {
        synchronized(frameStats) { frameStats.reset() }
        textureUploadLabView.resetCounters()
        statsText.text = FrameStatsSnapshot.empty().format(FRAME_NOTE)
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
}
