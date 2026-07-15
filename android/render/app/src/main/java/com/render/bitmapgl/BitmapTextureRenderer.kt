package com.render.bitmapgl

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Trace
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.Locale

/**
 * Owns every EGL/OpenGL object used by the lab. All methods must run on the same GL thread.
 */
internal class BitmapTextureRenderer(
    private val resources: Resources,
    private val jpegResourceId: Int,
    private val onEvent: (String) -> Unit,
    private val onStatus: (String) -> Unit
) {

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(QUAD_VERTICES.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(QUAD_VERTICES)
            position(0)
        }

    private var eglDisplay: EGLDisplay = EGL14.EGL_NO_DISPLAY
    private var eglContext: EGLContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface = EGL14.EGL_NO_SURFACE

    private var programId = 0
    private var textureId = 0
    private var positionLocation = -1
    private var textureCoordinateLocation = -1
    private var scaleLocation = -1
    private var samplerLocation = -1

    private var surfaceWidth = 0
    private var surfaceHeight = 0
    private var bitmapWidth = 0
    private var bitmapHeight = 0
    private var bitmapConfig = "-"
    private var maxTextureSize = 0
    private var frameCount = 0L
    private var lastDecodeNs = 0L
    private var lastUploadNs = 0L
    private var lastDrawCpuNs = 0L
    private var lastSwapCpuNs = 0L
    private var lastAction = "waiting for EGL"
    private var glRenderer = "-"

    fun initialize(outputSurface: Surface, width: Int, height: Int) {
        check(eglDisplay == EGL14.EGL_NO_DISPLAY) { "EGL is already initialized" }
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)

        Trace.beginSection("RTLab.BitmapGL.eglInitialize")
        try {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            check(eglDisplay != EGL14.EGL_NO_DISPLAY) { "eglGetDisplay failed: ${eglError()}" }

            val versions = IntArray(2)
            check(EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) {
                "eglInitialize failed: ${eglError()}"
            }

            val configAttributes = intArrayOf(
                EGL14.EGL_RED_SIZE, 8,
                EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
                EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val configCount = IntArray(1)
            check(
                EGL14.eglChooseConfig(
                    eglDisplay,
                    configAttributes,
                    0,
                    configs,
                    0,
                    configs.size,
                    configCount,
                    0
                ) && configCount[0] > 0
            ) { "eglChooseConfig failed: ${eglError()}" }
            val eglConfig = checkNotNull(configs[0]) { "eglChooseConfig returned no config" }

            eglContext = EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL14.EGL_NO_CONTEXT,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE),
                0
            )
            check(eglContext != EGL14.EGL_NO_CONTEXT) { "eglCreateContext failed: ${eglError()}" }

            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay,
                eglConfig,
                outputSurface,
                intArrayOf(EGL14.EGL_NONE),
                0
            )
            check(eglSurface != EGL14.EGL_NO_SURFACE) {
                "eglCreateWindowSurface failed: ${eglError()}"
            }
            check(
                EGL14.eglMakeCurrent(
                    eglDisplay,
                    eglSurface,
                    eglSurface,
                    eglContext
                )
            ) { "eglMakeCurrent failed: ${eglError()}" }

            programId = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            positionLocation = GLES20.glGetAttribLocation(programId, "aPosition")
            textureCoordinateLocation = GLES20.glGetAttribLocation(programId, "aTextureCoordinate")
            scaleLocation = GLES20.glGetUniformLocation(programId, "uScale")
            samplerLocation = GLES20.glGetUniformLocation(programId, "uTexture")
            check(
                positionLocation >= 0 && textureCoordinateLocation >= 0 &&
                    scaleLocation >= 0 && samplerLocation >= 0
            ) { "Required shader location was not found" }

            val maxTexture = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTexture, 0)
            maxTextureSize = maxTexture[0]
            glRenderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: "unknown"
            GLES20.glDisable(GLES20.GL_DEPTH_TEST)
            GLES20.glDisable(GLES20.GL_CULL_FACE)
            GLES20.glClearColor(0.035f, 0.055f, 0.09f, 1f)
            checkGlError("initialize GL state")

            lastAction = "EGL initialized"
            onEvent(
                "EGL/OpenGL ES 2.0 initialized on ${Thread.currentThread().name}; " +
                    "surface=${surfaceWidth}x$surfaceHeight, GL_RENDERER=$glRenderer."
            )
            emitStatus()
        } finally {
            Trace.endSection()
        }
    }

    fun decodeUploadAndDraw() {
        requireEglReady()

        val decodeStartNs = System.nanoTime()
        val bitmap = TraceSection("RTLab.BitmapGL.decodeJpeg").use {
            BitmapFactory.decodeResource(
                resources,
                jpegResourceId,
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                }
            ) ?: error("BitmapFactory could not decode bitmap_gl_sample.jpg")
        }
        lastDecodeNs = System.nanoTime() - decodeStartNs

        try {
            bitmapWidth = bitmap.width
            bitmapHeight = bitmap.height
            bitmapConfig = bitmap.config?.toString() ?: "unknown"
            check(bitmapWidth <= maxTextureSize && bitmapHeight <= maxTextureSize) {
                "JPEG ${bitmapWidth}x$bitmapHeight exceeds GL_MAX_TEXTURE_SIZE=$maxTextureSize"
            }

            Trace.beginSection("RTLab.BitmapGL.uploadTexture")
            val uploadStartNs = System.nanoTime()
            try {
                ensureTextureObject()
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
                GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1)
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
                checkGlError("GLUtils.texImage2D")
            } finally {
                lastUploadNs = System.nanoTime() - uploadStartNs
                Trace.endSection()
            }
        } finally {
            bitmap.recycle()
        }

        lastAction = "BitmapFactory decode + texImage2D upload"
        onEvent(
            "Decoded JPEG ${bitmapWidth}x$bitmapHeight ($bitmapConfig) in ${formatMs(lastDecodeNs)}, " +
                "then uploaded it to GL texture #$textureId in ${formatMs(lastUploadNs)}; CPU Bitmap recycled."
        )
        drawExistingTexture("after upload")
    }

    fun drawExistingTexture(reason: String = "reuse existing texture") {
        requireEglReady()
        if (textureId == 0) {
            onEvent("No GL texture exists yet; decode and upload the JPEG first.")
            return
        }

        Trace.beginSection("RTLab.BitmapGL.draw")
        val drawStartNs = System.nanoTime()
        try {
            GLES20.glViewport(0, 0, surfaceWidth, surfaceHeight)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(programId)

            val scale = aspectFitScale()
            GLES20.glUniform2f(scaleLocation, scale.first, scale.second)
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLES20.glUniform1i(samplerLocation, 0)

            vertexBuffer.position(POSITION_OFFSET_FLOATS)
            GLES20.glEnableVertexAttribArray(positionLocation)
            GLES20.glVertexAttribPointer(
                positionLocation,
                POSITION_COMPONENTS,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE_BYTES,
                vertexBuffer
            )

            vertexBuffer.position(TEXTURE_OFFSET_FLOATS)
            GLES20.glEnableVertexAttribArray(textureCoordinateLocation)
            GLES20.glVertexAttribPointer(
                textureCoordinateLocation,
                TEXTURE_COMPONENTS,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE_BYTES,
                vertexBuffer
            )

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX_COUNT)
            GLES20.glDisableVertexAttribArray(positionLocation)
            GLES20.glDisableVertexAttribArray(textureCoordinateLocation)
            checkGlError("glDrawArrays")
        } finally {
            lastDrawCpuNs = System.nanoTime() - drawStartNs
            Trace.endSection()
        }

        Trace.beginSection("RTLab.BitmapGL.eglSwapBuffers")
        val swapStartNs = System.nanoTime()
        try {
            check(EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
                "eglSwapBuffers failed: ${eglError()}"
            }
        } finally {
            lastSwapCpuNs = System.nanoTime() - swapStartNs
            Trace.endSection()
        }

        frameCount++
        lastAction = reason
        onEvent(
            "OpenGL drew frame #$frameCount with existing texture #$textureId; " +
                "draw CPU=${formatMs(lastDrawCpuNs)}, eglSwapBuffers CPU=${formatMs(lastSwapCpuNs)}."
        )
        emitStatus()
    }

    fun resize(width: Int, height: Int) {
        surfaceWidth = width.coerceAtLeast(1)
        surfaceHeight = height.coerceAtLeast(1)
        lastAction = "surface resized"
        onEvent("GL output surface resized to ${surfaceWidth}x$surfaceHeight.")
        if (textureId != 0) {
            drawExistingTexture("redraw after surface resize")
        } else {
            emitStatus()
        }
    }

    fun release() {
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) return
        Trace.beginSection("RTLab.BitmapGL.release")
        try {
            if (eglContext != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
                if (textureId != 0) {
                    GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                    textureId = 0
                }
                if (programId != 0) {
                    GLES20.glDeleteProgram(programId)
                    programId = 0
                }
            }

            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
            if (eglSurface != EGL14.EGL_NO_SURFACE) {
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
            }
            if (eglContext != EGL14.EGL_NO_CONTEXT) {
                EGL14.eglDestroyContext(eglDisplay, eglContext)
            }
            EGL14.eglTerminate(eglDisplay)
            EGL14.eglReleaseThread()
        } finally {
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
            lastAction = "EGL released"
            Trace.endSection()
        }
    }

    private fun ensureTextureObject() {
        if (textureId != 0) return
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        textureId = textureIds[0]
        check(textureId != 0) { "glGenTextures returned 0" }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
    }

    private fun aspectFitScale(): Pair<Float, Float> {
        if (bitmapWidth == 0 || bitmapHeight == 0) return 1f to 1f
        val surfaceAspect = surfaceWidth.toFloat() / surfaceHeight
        val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight
        return if (bitmapAspect > surfaceAspect) {
            1f to (surfaceAspect / bitmapAspect)
        } else {
            (bitmapAspect / surfaceAspect) to 1f
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        val program = GLES20.glCreateProgram()
        check(program != 0) { "glCreateProgram failed" }
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)

        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
        val infoLog = GLES20.glGetProgramInfoLog(program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
        if (linkStatus[0] != GLES20.GL_TRUE) {
            GLES20.glDeleteProgram(program)
            error("Could not link GL program: $infoLog")
        }
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        check(shader != 0) { "glCreateShader failed for type=$type" }
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] != GLES20.GL_TRUE) {
            val infoLog = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Could not compile shader type=$type: $infoLog")
        }
        return shader
    }

    private fun requireEglReady() {
        check(
            eglDisplay != EGL14.EGL_NO_DISPLAY &&
                eglContext != EGL14.EGL_NO_CONTEXT &&
                eglSurface != EGL14.EGL_NO_SURFACE
        ) { "EGL is not ready" }
    }

    private fun checkGlError(operation: String) {
        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) {
            "$operation failed with GL error 0x${Integer.toHexString(error)}"
        }
    }

    private fun eglError(): String =
        "0x${Integer.toHexString(EGL14.eglGetError())}"

    private fun emitStatus() {
        onStatus(
            "eglReady=${eglDisplay != EGL14.EGL_NO_DISPLAY}  surface=${surfaceWidth}x$surfaceHeight\n" +
                "jpeg=${bitmapWidth}x$bitmapHeight  bitmapConfig=$bitmapConfig\n" +
                "GL_MAX_TEXTURE_SIZE=$maxTextureSize  textureId=$textureId\n" +
                "frames=$frameCount  decode=${formatMs(lastDecodeNs)}  upload=${formatMs(lastUploadNs)}\n" +
                "drawCpu=${formatMs(lastDrawCpuNs)}  swapCpu=${formatMs(lastSwapCpuNs)}\n" +
                "lastAction=$lastAction\n" +
                "GL thread=${Thread.currentThread().name}\n" +
                "GL_RENDERER=$glRenderer"
        )
    }

    private fun formatMs(ns: Long): String =
        String.format(Locale.US, "%.2fms", ns / 1_000_000.0)

    private class TraceSection(private val name: String) : AutoCloseable {
        init {
            Trace.beginSection(name)
        }

        override fun close() {
            Trace.endSection()
        }
    }

    companion object {
        private const val POSITION_COMPONENTS = 2
        private const val TEXTURE_COMPONENTS = 2
        private const val FLOATS_PER_VERTEX = POSITION_COMPONENTS + TEXTURE_COMPONENTS
        private const val VERTEX_STRIDE_BYTES = FLOATS_PER_VERTEX * Float.SIZE_BYTES
        private const val POSITION_OFFSET_FLOATS = 0
        private const val TEXTURE_OFFSET_FLOATS = POSITION_COMPONENTS
        private const val VERTEX_COUNT = 4

        // Texture Y is intentionally flipped: Bitmap row 0 is the visual top, while GL t=0 is bottom.
        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f
        )

        private const val VERTEX_SHADER = """
            attribute vec2 aPosition;
            attribute vec2 aTextureCoordinate;
            uniform vec2 uScale;
            varying vec2 vTextureCoordinate;

            void main() {
                gl_Position = vec4(aPosition * uScale, 0.0, 1.0);
                vTextureCoordinate = aTextureCoordinate;
            }
        """

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D uTexture;
            varying vec2 vTextureCoordinate;

            void main() {
                gl_FragColor = texture2D(uTexture, vTextureCoordinate);
            }
        """
    }
}
