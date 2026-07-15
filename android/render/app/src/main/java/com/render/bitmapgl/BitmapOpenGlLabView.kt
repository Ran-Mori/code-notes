package com.render.bitmapgl

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Trace
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import com.render.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * TextureView lifecycle owner. It turns SurfaceTexture into a producer Surface and gives that
 * Surface to a dedicated EGL/OpenGL thread.
 */
class BitmapOpenGlLabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onEvent: (String) -> Unit = {},
    private val onStatus: (String) -> Unit = {}
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val textureUpdates = AtomicLong(0L)

    @Volatile
    private var rendererStatus = "EGL renderer has not started."

    private var hostResumed = false
    private var surfaceReady = false
    private var surfaceWidthPx = 0
    private var surfaceHeightPx = 0
    private var glThread: HandlerThread? = null
    private var glHandler: Handler? = null
    private var renderer: BitmapTextureRenderer? = null
    private var producerSurface: Surface? = null

    init {
        surfaceTextureListener = this
        isOpaque = true
    }

    fun onHostResume() {
        hostResumed = true
        if (isAvailable && glThread == null) {
            surfaceTexture?.let { startRenderer(it, width, height) }
        }
        emitStatus()
    }

    fun onHostPause() {
        hostResumed = false
        stopRenderer("Activity paused")
        emitStatus()
    }

    fun release() {
        hostResumed = false
        stopRenderer("Activity destroyed")
    }

    fun decodeUploadAndDraw() {
        postRendererAction("decode/upload") {
            decodeUploadAndDraw()
        }
    }

    fun drawExistingTexture() {
        postRendererAction("draw existing texture") {
            drawExistingTexture()
        }
    }

    fun currentStatus(): String =
        "surfaceReady=$surfaceReady  hostResumed=$hostResumed  size=${surfaceWidthPx}x$surfaceHeightPx\n" +
            "onSurfaceTextureUpdated=${textureUpdates.get()}  producerThread=${glThread?.name ?: "-"}\n" +
            rendererStatus

    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        Trace.beginSection("RTLab.BitmapGL.onSurfaceTextureAvailable")
        try {
            surfaceReady = true
            surfaceWidthPx = width
            surfaceHeightPx = height
            emitEvent(
                "TextureView SurfaceTexture available on ${Thread.currentThread().name}; " +
                    "size=${width}x$height."
            )
            if (hostResumed) {
                startRenderer(surfaceTexture, width, height)
            }
            emitStatus()
        } finally {
            Trace.endSection()
        }
    }

    override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        surfaceWidthPx = width
        surfaceHeightPx = height
        postRendererAction("resize") {
            resize(width, height)
        }
        emitStatus()
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
        surfaceReady = false
        surfaceWidthPx = 0
        surfaceHeightPx = 0
        stopRenderer("SurfaceTexture destroyed")
        emitEvent(
            "SurfaceTexture destroyed on ${Thread.currentThread().name}; " +
                "EGLSurface and producer Surface were released first."
        )
        emitStatus()
        return true
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
        val updates = textureUpdates.incrementAndGet()
        if (updates == 1L || updates % 10L == 0L) {
            emitEvent("TextureView consumed GL output buffer #$updates on ${Thread.currentThread().name}.")
        }
        emitStatus()
    }

    private fun startRenderer(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        if (glThread != null) return
        val outputSurface = Surface(surfaceTexture)
        val thread = HandlerThread("BitmapOpenGlProducer").apply { start() }
        val handler = Handler(thread.looper)
        producerSurface = outputSurface
        glThread = thread
        glHandler = handler
        rendererStatus = "Starting EGL on ${thread.name}..."
        emitStatus()

        handler.post {
            val activeRenderer = BitmapTextureRenderer(
                resources = resources,
                jpegResourceId = R.drawable.bitmap_gl_sample,
                onEvent = ::emitEvent,
                onStatus = ::updateRendererStatus
            )
            renderer = activeRenderer
            try {
                activeRenderer.initialize(outputSurface, width, height)
                activeRenderer.decodeUploadAndDraw()
            } catch (error: Throwable) {
                rendererStatus = "Renderer error: ${error.message ?: error.javaClass.simpleName}"
                emitEvent("Bitmap/OpenGL renderer failed: ${error.message ?: error.javaClass.simpleName}.")
                emitStatus()
                activeRenderer.release()
            }
        }
    }

    private fun postRendererAction(
        label: String,
        action: BitmapTextureRenderer.() -> Unit
    ) {
        val handler = glHandler
        if (handler == null) {
            emitEvent("Cannot $label: wait for a resumed Activity and an available SurfaceTexture.")
            return
        }
        handler.post {
            val activeRenderer = renderer
            if (activeRenderer == null) {
                emitEvent("Cannot $label: EGL renderer is not ready yet.")
                return@post
            }
            try {
                activeRenderer.action()
            } catch (error: Throwable) {
                rendererStatus = "$label failed: ${error.message ?: error.javaClass.simpleName}"
                emitEvent("Renderer action '$label' failed: ${error.message ?: error.javaClass.simpleName}.")
                emitStatus()
            }
        }
    }

    private fun stopRenderer(reason: String) {
        val thread = glThread ?: return
        val handler = glHandler
        val surface = producerSurface
        val releaseFinished = CountDownLatch(1)
        val posted = handler?.post {
            try {
                renderer?.release()
                renderer = null
                surface?.release()
            } finally {
                releaseFinished.countDown()
            }
        } == true

        thread.quitSafely()
        val releasedInTime = if (posted) {
            releaseFinished.await(RELEASE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        } else {
            surface?.release()
            true
        }
        thread.join(THREAD_JOIN_TIMEOUT_MS)

        glThread = null
        glHandler = null
        producerSurface = null
        rendererStatus = if (releasedInTime) {
            "EGL renderer stopped: $reason"
        } else {
            "Timed out while stopping EGL renderer: $reason"
        }
        emitEvent("BitmapOpenGlProducer stopped: $reason.")
    }

    private fun updateRendererStatus(status: String) {
        rendererStatus = status
        emitStatus()
    }

    private fun emitStatus() {
        mainHandler.post {
            onStatus(currentStatus())
        }
    }

    private fun emitEvent(message: String) {
        mainHandler.post {
            onEvent(message)
        }
    }

    companion object {
        private const val RELEASE_TIMEOUT_MS = 1_500L
        private const val THREAD_JOIN_TIMEOUT_MS = 250L
    }
}
