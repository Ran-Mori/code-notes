package com.baj

import android.graphics.Bitmap
import android.graphics.Paint
import android.util.Log
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.request.BasePostprocessor

class BigMosaicProcessor: BasePostprocessor() {
    override fun process(
        sourceBitmap: Bitmap?,
        bitmapFactory: PlatformBitmapFactory?
    ): CloseableReference<Bitmap>? {
        val startTime = System.currentTimeMillis()

        sourceBitmap ?: return null
        bitmapFactory ?: return null

        val width = sourceBitmap.width
        val height = sourceBitmap.height

        if (width <= 16 && height <= 16) {
            return bitmapFactory.createBitmap(sourceBitmap)
        }

        val mosaicWidth = width / 16
        val mosaicHeight = height / 16

        val array = Array(16) { intArrayOf(16) }

        for (i in 0 until 16) {
            for (j in 0 until 16) {
                array[i][j] = sourceBitmap[i * width, j * height]
            }
        }

        val mosaicBitmapReference = bitmapFactory.createBitmapInternal(
            width,
            height,
            sourceBitmap.config
        )

        for (i in 0 until width) {
            for (j in 0 until height) {
                Log.d("IzumiSakai", "BigMosaicProcessor foreach i = ${i}, j = ${j}")
                mosaicBitmapReference.get()[i, j] = array[i / mosaicWidth][j / mosaicHeight]
            }
        }

        Log.d("IzumiSakai", "BigMosaicProcessor process time = ${System.currentTimeMillis() - startTime}")
        return mosaicBitmapReference
    }

    override fun getName(): String = "BigMosaicProcessor"

    override fun getPostprocessorCacheKey(): CacheKey  = SimpleCacheKey("BigMosaicProcessor")
}