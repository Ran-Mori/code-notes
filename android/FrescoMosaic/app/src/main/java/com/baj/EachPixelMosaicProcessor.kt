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

class EachPixelMosaicProcessor: BasePostprocessor() {

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

        val mosaicWidth = sourceWidth / SIZE + 1
        val mosaicHeight = sourceHeight / SIZE + 1

        val array = Array(SIZE) { Array(SIZE) {0} }

        for (i in 0 until SIZE) {
            for (j in 0 until SIZE) {
                array[i][j] = sourceBitmap[i * mosaicWidth, j * mosaicHeight]
            }
        }

        val mosaicBitmapReference = bitmapFactory.createBitmapInternal(
            sourceWidth,
            sourceHeight,
            sourceBitmap.config
        )

        for (i in 0 until sourceWidth) {
            for (j in 0 until sourceHeight) {
//                Log.d(MainActivity.TAG, "EachPixelMosaicProcessor foreach i = ${i}, j = ${j}")
                mosaicBitmapReference.get()[i, j] = array[i / mosaicWidth][j / mosaicHeight]
            }
        }

        Log.d(MainActivity.TAG, "EachPixelMosaicProcessor process time = ${System.currentTimeMillis() - startTime}")
        return mosaicBitmapReference
    }

    override fun getName(): String = "EachPixelMosaicProcessor"

    override fun getPostprocessorCacheKey(): CacheKey  = SimpleCacheKey("EachPixelMosaicProcessor")
}