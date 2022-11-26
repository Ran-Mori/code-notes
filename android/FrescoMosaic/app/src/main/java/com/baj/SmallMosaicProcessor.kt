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

class SmallMosaicProcessor: BasePostprocessor() {
    override fun process(
        sourceBitmap: Bitmap?,
        bitmapFactory: PlatformBitmapFactory?
    ): CloseableReference<Bitmap>? {
        val startTime = System.currentTimeMillis()

        sourceBitmap ?: return null
        bitmapFactory ?: return null

        val width = sourceBitmap.width
        val height = sourceBitmap.height

        val mosaicWidth = width / 16
        val mosaicHeight = height / 16

        if (width <= 16 && height <= 16) {
            Paint.FILTER_BITMAP_FLAG
            return bitmapFactory.createBitmap(sourceBitmap)
        }

        val mosaicBitmapReference = bitmapFactory.createBitmapInternal(
            16,
            16,
            sourceBitmap.config
        )

        for (i in 0 until 16) {
            for (j in 0 until 16) {
                mosaicBitmapReference.get()[i, j] = sourceBitmap[i * mosaicWidth, j * mosaicHeight]
            }
        }

        Log.d("IzumiSakai", "SmallMosaicProcessor process time = ${System.currentTimeMillis() - startTime}")
        return mosaicBitmapReference
    }

    override fun getName(): String = "SmallMosaicProcessor"

    override fun getPostprocessorCacheKey(): CacheKey  = SimpleCacheKey("SmallMosaicProcessor")
}