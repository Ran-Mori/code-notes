package com.baj

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.get
import androidx.core.graphics.set
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory
import com.facebook.imagepipeline.request.BasePostprocessor

class ScaledMosaicProcessor: BasePostprocessor() {

    companion object {
        const val SIZE = 16
    }

    override fun process(
        sourceBitmap: Bitmap?,
        bitmapFactory: PlatformBitmapFactory?
    ): CloseableReference<Bitmap>? {
        val startTime = System.currentTimeMillis()

        sourceBitmap ?: return null
        bitmapFactory ?: return null

        val sourceWidth = sourceBitmap.width
        val sourceHeight = sourceBitmap.height

        if (sourceWidth <= SIZE && sourceHeight <= SIZE) {
            return bitmapFactory.createBitmap(sourceBitmap)
        }

        val mosaicWidth = sourceWidth / SIZE
        val mosaicHeight = sourceHeight / SIZE

        val mosaicBitmapReference = bitmapFactory.createBitmapInternal(
            SIZE,
            SIZE,
            sourceBitmap.config
        )

        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                mosaicBitmapReference.get()[i, j] = sourceBitmap[i * mosaicWidth, j * mosaicHeight]
            }
        }

        val scaledMosaicBitmapReference = bitmapFactory.createScaledBitmap(mosaicBitmapReference.get(), sourceWidth, sourceHeight, true)

        Log.d(MainActivity.TAG, "ScaledMosaicProcessor process time = ${System.currentTimeMillis() - startTime}")
        return scaledMosaicBitmapReference
    }

    override fun getName(): String = "ScaledMosaicProcessor"

    override fun getPostprocessorCacheKey(): CacheKey  = SimpleCacheKey("ScaledMosaicProcessor")
}