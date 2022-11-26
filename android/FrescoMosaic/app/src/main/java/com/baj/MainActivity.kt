package com.baj

import android.graphics.drawable.Animatable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.listener.BaseRequestListener
import com.facebook.imagepipeline.request.ImageRequest
import com.facebook.imagepipeline.request.ImageRequestBuilder

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var sdvView: SimpleDraweeView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sdvView = findViewById(R.id.sdv_view)

        // IzumiSakai -> https://b-ssl.duitang.com/uploads/item/201707/03/20170703231208_GEQws.thumb.700_0.jpeg
        // KID -> https://pic3.zhimg.com/v2-594d525c95d615fceb6f9f3aabdca76a_r.jpg?source=1940ef5c

        val mController = Fresco.newDraweeControllerBuilder()
            .setImageRequest(
                ImageRequestBuilder.newBuilderWithSource(Uri.parse("https://pic3.zhimg.com/v2-594d525c95d615fceb6f9f3aabdca76a_r.jpg?source=1940ef5c"))
                    .setPostprocessor(ScaledMosaicProcessor())
//                    .setPostprocessor(EachPixelMosaicProcessor())
                    .setRequestListener(object : BaseRequestListener() {
                        private var startTime: Long = 0
                        override fun onRequestStart(
                            request: ImageRequest?,
                            callerContext: Any?,
                            requestId: String?,
                            isPrefetch: Boolean
                        ) {
                            super.onRequestStart(request, callerContext, requestId, isPrefetch)
                            startTime = System.currentTimeMillis()
                        }

                        override fun onRequestSuccess(
                            request: ImageRequest?,
                            requestId: String?,
                            isPrefetch: Boolean
                        ) {
                            super.onRequestSuccess(request, requestId, isPrefetch)
                            Log.d(TAG, "ImageRequest spend time = ${System.currentTimeMillis() - startTime}")
                        }
                    })
                    .build()
            )
            .setControllerListener(object: BaseControllerListener<ImageInfo>() {
                override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, animatable: Animatable?) {
                    Log.d(TAG,"onFinalImageSet")
                }

                override fun onFailure(id: String?, throwable: Throwable?) {
                    Log.d(TAG,"onFailure")
                }
            })
            .build()

        sdvView?.controller = mController
    }
}