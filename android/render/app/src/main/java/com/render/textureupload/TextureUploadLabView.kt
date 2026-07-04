package com.render.textureupload

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Trace
import android.util.AttributeSet
import android.view.View
import java.util.Locale
import kotlin.math.max

class TextureUploadLabView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val onEvent: (String) -> Unit = {}
) : View(context, attrs, defStyleAttr) {

    var continuousDirtyUpload: Boolean = false
    var drawCount: Long = 0L
        private set

    private val textures = mutableListOf<Bitmap>()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 252, 247)
    }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(226, 217, 204)
        strokeWidth = dp(1.2f)
        style = Paint.Style.STROKE
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(58, 50, 42)
        textSize = sp(12f)
    }
    private val strongLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(28, 24, 21)
        textSize = sp(13.5f)
        isFakeBoldText = true
    }
    private val texturePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tileRect = RectF()

    private var dirtyGeneration = 0L
    private var lastDrawnGeneration = -1L
    private var lastLoggedDraw = 0L

    init {
        setWillNotDraw(false)
    }

    fun advance(frameTimeNanos: Long) {
        if (!continuousDirtyUpload || textures.isEmpty()) return
        Trace.beginSection("RTLab.Texture.markDirtyEachFrame(main)")
        try {
            markTexturesDirty(frameTimeNanos)
        } finally {
            Trace.endSection()
        }
        postInvalidateOnAnimation()
    }

    fun prepareTexturesIfNeeded(): Boolean {
        if (textures.isNotEmpty()) return true
        return prepareTextures()
    }

    fun prepareTextures(): Boolean {
        Trace.beginSection("RTLab.Texture.prepareCpuBitmaps(main)")
        return try {
            releaseTextures(logEvent = false)
            repeat(TEXTURE_COUNT) { index ->
                textures += Bitmap.createBitmap(TEXTURE_SIZE_PX, TEXTURE_SIZE_PX, Bitmap.Config.ARGB_8888).also {
                    drawInitialPattern(it, index)
                }
            }
            dirtyGeneration = 1L
            lastDrawnGeneration = -1L
            invalidate()
            onEvent(
                "Prepared $TEXTURE_COUNT mutable ${TEXTURE_SIZE_PX}x$TEXTURE_SIZE_PX ARGB_8888 bitmaps " +
                    "(${formatMb(totalTextureBytes())}). First draw or dirty draw forces HWUI texture upload."
            )
            true
        } catch (error: OutOfMemoryError) {
            releaseTextures(logEvent = false)
            onEvent("Failed to allocate texture bitmaps: ${error.message ?: "out of memory"}.")
            false
        } finally {
            Trace.endSection()
        }
    }

    fun requestSingleUpload() {
        if (!prepareTexturesIfNeeded()) return
        Trace.beginSection("RTLab.Texture.singleDirtyUpload(main)")
        try {
            markTexturesDirty(System.nanoTime())
        } finally {
            Trace.endSection()
        }
        postInvalidateOnAnimation()
        onEvent(
            "Marked $TEXTURE_COUNT large bitmaps dirty by changing 1 pixel each; " +
                "next draw records drawBitmap, then RenderThread/HWUI has to upload fresh texture content."
        )
    }

    fun resetCounters() {
        drawCount = 0L
        lastLoggedDraw = 0L
        lastDrawnGeneration = -1L
        invalidate()
    }

    fun releaseTextures(logEvent: Boolean = true) {
        continuousDirtyUpload = false
        textures.forEach { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        textures.clear()
        dirtyGeneration = 0L
        lastDrawnGeneration = -1L
        invalidate()
        if (logEvent) {
            onEvent("Texture bitmaps released; continuous dirty upload stopped.")
        }
    }

    override fun onDraw(canvas: Canvas) {
        val startNs = System.nanoTime()
        Trace.beginSection("RTLab.Texture.drawBitmap(record)")
        try {
            drawCount++
            drawScene(canvas)
        } finally {
            Trace.endSection()
        }

        val durationMs = (System.nanoTime() - startNs) / 1_000_000.0
        val drewDirtyGeneration = textures.isNotEmpty() && dirtyGeneration != lastDrawnGeneration
        if (drewDirtyGeneration || drawCount == 1L || drawCount - lastLoggedDraw >= 45L) {
            lastLoggedDraw = drawCount
            lastDrawnGeneration = dirtyGeneration
            val countSnapshot = drawCount
            val generationSnapshot = dirtyGeneration
            post {
                onEvent(
                    "Texture view onDraw #$countSnapshot recorded drawBitmap in " +
                        "${String.format(Locale.US, "%.2f", durationMs)}ms; dirtyGeneration=$generationSnapshot"
                )
            }
        }
    }

    private fun drawScene(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)
        canvas.drawText("Texture upload lab: dirty Bitmap -> drawBitmap -> HWUI texture upload", dp(14f), dp(27f), strongLabelPaint)
        canvas.drawText(
            "Textures=${textures.size}  size=${TEXTURE_SIZE_PX}px  dirtyGen=$dirtyGeneration  continuous=$continuousDirtyUpload",
            dp(14f),
            dp(49f),
            labelPaint
        )

        if (textures.isEmpty()) {
            drawEmptyState(canvas)
            return
        }

        val gap = dp(8f)
        val left = dp(14f)
        val right = width - dp(14f)
        val top = dp(68f)
        val bottom = height - dp(32f)
        val tileWidth = max(dp(52f), (right - left - gap * (textures.size - 1)) / textures.size)
        textures.forEachIndexed { index, bitmap ->
            val tileLeft = left + index * (tileWidth + gap)
            tileRect.set(tileLeft, top, tileLeft + tileWidth, bottom)
            canvas.drawBitmap(bitmap, null, tileRect, texturePaint)
            canvas.drawRoundRect(tileRect, dp(8f), dp(8f), borderPaint)
            canvas.drawText("tex#$index", tileRect.left + dp(8f), tileRect.bottom - dp(8f), strongLabelPaint)
        }
    }

    private fun drawEmptyState(canvas: Canvas) {
        val left = dp(14f)
        val top = dp(68f)
        val right = width - dp(14f)
        val bottom = height - dp(32f)
        tileRect.set(left, top, right, bottom)
        canvas.drawRoundRect(tileRect, dp(10f), dp(10f), borderPaint)
        canvas.drawText("Tap prepare, then record Perfetto while triggering upload.", left + dp(14f), top + dp(36f), labelPaint)
    }

    private fun drawInitialPattern(bitmap: Bitmap, index: Int) {
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val baseColor = when (index % 3) {
            0 -> Color.rgb(223, 79, 64)
            1 -> Color.rgb(35, 132, 87)
            else -> Color.rgb(42, 103, 210)
        }
        canvas.drawColor(baseColor)
        paint.color = Color.argb(110, 255, 255, 255)
        val step = TEXTURE_SIZE_PX / 16f
        var i = 0
        while (i <= 16) {
            val p = i * step
            canvas.drawRect(p, 0f, p + step / 3f, TEXTURE_SIZE_PX.toFloat(), paint)
            canvas.drawRect(0f, p, TEXTURE_SIZE_PX.toFloat(), p + step / 4f, paint)
            i += 2
        }
        paint.color = Color.argb(150, 20, 20, 20)
        paint.textSize = TEXTURE_SIZE_PX / 7f
        paint.isFakeBoldText = true
        canvas.drawText("GPU", TEXTURE_SIZE_PX * 0.18f, TEXTURE_SIZE_PX * 0.55f, paint)
        markOnePixel(bitmap, index, dirtyGeneration)
    }

    private fun markTexturesDirty(seed: Long) {
        dirtyGeneration++
        textures.forEachIndexed { index, bitmap ->
            markOnePixel(bitmap, index, seed + dirtyGeneration)
        }
    }

    private fun markOnePixel(bitmap: Bitmap, index: Int, seed: Long) {
        if (bitmap.isRecycled) return
        val x = ((seed + index * 97L) % TEXTURE_SIZE_PX).toInt().coerceAtLeast(0)
        val y = ((seed / 7L + index * 131L) % TEXTURE_SIZE_PX).toInt().coerceAtLeast(0)
        val color = Color.rgb(
            ((seed + index * 41L).and(0xFFL)).toInt(),
            ((seed / 3L + 80L).and(0xFFL)).toInt(),
            ((seed / 5L + 160L).and(0xFFL)).toInt()
        )
        bitmap.setPixel(x, y, color)
    }

    private fun totalTextureBytes(): Long =
        TEXTURE_COUNT.toLong() * TEXTURE_SIZE_PX * TEXTURE_SIZE_PX * BYTES_PER_ARGB_8888

    private fun formatMb(bytes: Long): String =
        String.format(Locale.US, "%.1fMB", bytes / 1024.0 / 1024.0)

    private fun dp(value: Float): Float =
        value * resources.displayMetrics.density

    private fun sp(value: Float): Float =
        value * resources.displayMetrics.scaledDensity

    companion object {
        private const val TEXTURE_COUNT = 3
        private const val TEXTURE_SIZE_PX = 2048
        private const val BYTES_PER_ARGB_8888 = 4
    }
}
