package com.render.textureview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Trace
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class TextureViewLabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onEvent: (String) -> Unit = {},
    private val onStatus: (String) -> Unit = {}
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val surfaceLock = Object()
    private val producerFrames = AtomicLong(0L)
    private val textureUpdates = AtomicLong(0L)

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(20, 25, 32)
        style = Paint.Style.FILL
    }
    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 43, 54)
        style = Paint.Style.FILL
    }
    private val activeBufferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(39, 161, 120)
        style = Paint.Style.FILL
    }
    private val queuedBufferPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(62, 78, 97)
        style = Paint.Style.FILL
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 191, 71)
        style = Paint.Style.FILL
    }
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(112, 130, 153)
        strokeWidth = dp(1.2f)
        style = Paint.Style.STROKE
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = sp(16f)
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(220, 228, 238)
        textSize = sp(12f)
    }
    private val dimLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(158, 171, 188)
        textSize = sp(11f)
    }
    private val rect = RectF()

    @Volatile
    private var surfaceReady = false
    @Volatile
    private var surfaceWidthPx = 0
    @Volatile
    private var surfaceHeightPx = 0
    @Volatile
    private var continuousProducer = false
    @Volatile
    private var lastProducerThread = "-"
    @Volatile
    private var lastUpdateThread = "-"
    @Volatile
    private var lastLockCanvasMs = "0.00ms"

    private var surface: Surface? = null
    private var producerThread: HandlerThread? = null
    private var producerHandler: Handler? = null

    private val continuousDrawRunnable = object : Runnable {
        override fun run() {
            if (!continuousProducer) return
            drawProducerFrame(reason = "continuous")
            producerHandler?.postDelayed(this, FRAME_DELAY_MS)
        }
    }

    init {
        surfaceTextureListener = this
    }

    fun drawSingleFrame() {
        ensureProducerThread()
        producerHandler?.post {
            drawProducerFrame(reason = "single")
        }
    }

    fun startContinuousProducer() {
        if (continuousProducer) return
        continuousProducer = true
        ensureProducerThread()
        emitEvent("TextureView producer loop started on TextureViewProducer.")
        producerHandler?.post(continuousDrawRunnable)
        emitStatus()
    }

    fun stopContinuousProducer(logEvent: Boolean = true) {
        if (!continuousProducer) return
        continuousProducer = false
        producerHandler?.removeCallbacks(continuousDrawRunnable)
        if (logEvent) {
            emitEvent("TextureView producer loop stopped.")
        }
        emitStatus()
    }

    fun resetCounters() {
        producerFrames.set(0L)
        textureUpdates.set(0L)
        lastLockCanvasMs = "0.00ms"
        emitStatus()
        emitEvent("TextureView counters reset.")
    }

    fun releaseProducer() {
        stopContinuousProducer(logEvent = false)
        synchronized(surfaceLock) {
            surface?.release()
            surface = null
        }
        producerThread?.quitSafely()
        producerThread = null
        producerHandler = null
        surfaceReady = false
        emitStatus()
    }

    fun currentStatus(): String =
        "surfaceReady=$surfaceReady  size=${surfaceWidthPx}x$surfaceHeightPx\n" +
            "producerFrames=${producerFrames.get()}  onSurfaceTextureUpdated=${textureUpdates.get()}\n" +
            "continuousProducer=$continuousProducer  lastLockCanvas=$lastLockCanvasMs\n" +
            "producerThread=$lastProducerThread\n" +
            "updateCallbackThread=$lastUpdateThread"

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        Trace.beginSection("RTLab.TextureView.onSurfaceTextureAvailable")
        try {
            synchronized(surfaceLock) {
                surface?.release()
                surface = Surface(surfaceTexture)
            }
            surfaceReady = true
            surfaceWidthPx = width
            surfaceHeightPx = height
            emitEvent(
                "SurfaceTexture available on ${Thread.currentThread().name}; " +
                    "created one producer Surface from it."
            )
            drawSingleFrame()
            emitStatus()
        } finally {
            Trace.endSection()
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surfaceWidthPx = width
        surfaceHeightPx = height
        emitEvent("SurfaceTexture size changed to ${width}x$height on ${Thread.currentThread().name}.")
        drawSingleFrame()
        emitStatus()
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        emitEvent("SurfaceTexture destroyed on ${Thread.currentThread().name}; release producer Surface.")
        releaseProducer()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        lastUpdateThread = Thread.currentThread().name
        val updated = textureUpdates.incrementAndGet()
        if (updated == 1L || updated % 30L == 0L) {
            emitEvent("onSurfaceTextureUpdated #$updated on $lastUpdateThread.")
        }
        emitStatus(throttle = true)
    }

    private fun ensureProducerThread() {
        if (producerThread?.isAlive == true && producerHandler != null) return
        producerThread = HandlerThread("TextureViewProducer").apply { start() }
        producerHandler = Handler(producerThread!!.looper)
    }

    private fun drawProducerFrame(reason: String) {
        var missingSurface = false
        var postedFrame = false
        var frameNo = 0L
        Trace.beginSection("RTLab.TextureView.producerFrame($reason)")
        try {
            synchronized(surfaceLock) {
                val activeSurface = surface
                if (activeSurface == null || !activeSurface.isValid) {
                    missingSurface = true
                    return@synchronized
                }

                frameNo = producerFrames.incrementAndGet()
                val lockStartNs = System.nanoTime()
                val canvas = activeSurface.lockCanvas(null)
                val lockNs = System.nanoTime() - lockStartNs
                lastLockCanvasMs = formatMs(lockNs)
                try {
                    drawInto(canvas, frameNo, reason, lockNs)
                } finally {
                    activeSurface.unlockCanvasAndPost(canvas)
                }
                postedFrame = true
            }
        } catch (error: RuntimeException) {
            emitEvent("Producer failed to draw into TextureView Surface: ${error.message ?: error.javaClass.simpleName}.")
        } finally {
            Trace.endSection()
        }

        if (missingSurface) {
            emitEvent("Producer has no valid Surface yet. Wait for onSurfaceTextureAvailable.")
            return
        }
        if (postedFrame && (frameNo == 1L || frameNo % 30L == 0L || reason == "single")) {
            lastProducerThread = Thread.currentThread().name
            emitEvent("Producer posted frame #$frameNo by lockCanvas/unlockCanvasAndPost on $lastProducerThread.")
        }
        emitStatus(throttle = true)
    }

    private fun drawInto(canvas: Canvas, frameNo: Long, reason: String, lockNs: Long) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        canvas.drawRect(0f, 0f, w, h, backgroundPaint)

        val left = dp(16f)
        val top = dp(18f)
        canvas.drawText("TextureView = View + SurfaceTexture", left, top + dp(4f), titlePaint)
        canvas.drawText(
            "producer: Surface.lockCanvas -> unlockCanvasAndPost",
            left,
            top + dp(27f),
            labelPaint
        )

        val queueTop = top + dp(54f)
        val queueHeight = dp(58f)
        val gap = dp(8f)
        val slotWidth = (w - left * 2f - gap * 2f) / 3f
        val activeSlot = (frameNo % 3L).toInt()
        repeat(3) { index ->
            val slotLeft = left + index * (slotWidth + gap)
            rect.set(slotLeft, queueTop, slotLeft + slotWidth, queueTop + queueHeight)
            canvas.drawRoundRect(rect, dp(10f), dp(10f), if (index == activeSlot) activeBufferPaint else queuedBufferPaint)
            canvas.drawText("buffer $index", slotLeft + dp(10f), queueTop + dp(25f), labelPaint)
            canvas.drawText(
                if (index == activeSlot) "newest" else "queued",
                slotLeft + dp(10f),
                queueTop + dp(45f),
                dimLabelPaint
            )
        }

        val progress = ((frameNo % 120L).toFloat() / 119f).coerceIn(0f, 1f)
        val movingX = lerp(left + dp(22f), w - left - dp(22f), if ((frameNo / 120L) % 2L == 0L) progress else 1f - progress)
        val movingY = queueTop + queueHeight + dp(44f)
        canvas.drawCircle(movingX, movingY, dp(15f), accentPaint)
        canvas.drawLine(left, movingY, w - left, movingY, linePaint)

        val bottomPanelTop = h - dp(64f)
        rect.set(left, bottomPanelTop, w - left, h - dp(14f))
        canvas.drawRoundRect(rect, dp(10f), dp(10f), panelPaint)
        canvas.drawText(
            "frame=$frameNo  reason=$reason  lockCanvas=${formatMs(lockNs)}",
            left + dp(12f),
            bottomPanelTop + dp(22f),
            labelPaint
        )
        canvas.drawText(
            "TextureView later samples newest buffer as a GLES texture in the View tree",
            left + dp(12f),
            bottomPanelTop + dp(42f),
            dimLabelPaint
        )
    }

    private var lastStatusUptimeMs = 0L

    private fun emitStatus(throttle: Boolean = false) {
        val now = android.os.SystemClock.uptimeMillis()
        if (throttle && now - lastStatusUptimeMs < STATUS_THROTTLE_MS) return
        lastStatusUptimeMs = now
        mainHandler.post {
            onStatus(currentStatus())
        }
    }

    private fun emitEvent(message: String) {
        mainHandler.post {
            onEvent(message)
        }
    }

    private fun formatMs(ns: Long): String =
        String.format(Locale.US, "%.2fms", ns / 1_000_000.0)

    private fun lerp(start: Float, end: Float, t: Float): Float =
        start + (end - start) * t

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity

    companion object {
        private const val FRAME_DELAY_MS = 16L
        private const val STATUS_THROTTLE_MS = 250L
    }
}
