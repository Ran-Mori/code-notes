package com.lottie

import android.animation.Animator
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import com.airbnb.lottie.ImageAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieImageAsset

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
        const val CLIP_CHILDREN = 1
        const val SCALE = 2
    }

    private var strategy: Int = SCALE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = when(strategy) {
            CLIP_CHILDREN -> R.layout.activity_clip_children
            SCALE -> R.layout.activity_scale
            else -> 0
        }

        setContentView(layoutId)

        when(strategy) {
            CLIP_CHILDREN -> initNormal()
            SCALE -> initOverlay()
        }
    }

    private fun initNormal() {
        val lottieView: LottieAnimationView? by lazy { findViewById(R.id.lottie_view) }
        val imageView: ImageView? by lazy { findViewById(R.id.red_packet_icon) }
        imageView?.setOnClickListener {
            lottieView?.setImageAssetDelegate(object : ImageAssetDelegate {
                override fun fetchBitmap(lottieAsset: LottieImageAsset?): Bitmap? {
                    lottieAsset ?: return null
                    val opts = BitmapFactory.Options()
                    opts.inScaled = true
                    opts.inDensity = applicationContext.resources.displayMetrics.densityDpi
                    return BitmapFactory.decodeStream(assets.open("lottie/images/${lottieAsset.fileName}"), null, opts)
                }
            })
            LottieCompositionFactory.fromJsonInputStream(assets.open("lottie/data.json"), "cacheKey")
                .addListener { result: LottieComposition ->
                    lottieView?.apply {
                        setComposition(result)
                        playAnimation()
                    }
                }
        }
    }

    private fun initOverlay() {
        val lottieView: LottieAnimationView? by lazy { findViewById(R.id.lottie_view) }
        val imageView: ImageView? by lazy { findViewById(R.id.red_packet_icon) }
        val scale = 100f / 44f
        imageView?.setOnClickListener {
            lottieView?.setImageAssetDelegate(object : ImageAssetDelegate {
                override fun fetchBitmap(lottieAsset: LottieImageAsset?): Bitmap? {
                    lottieAsset ?: return null
                    val opts = BitmapFactory.Options()
                    opts.inScaled = true
                    opts.inDensity = applicationContext.resources.displayMetrics.densityDpi
                    // 取图片来做动画
                    return BitmapFactory.decodeStream(assets.open("lottie/images/${lottieAsset.fileName}"), null, opts)
                }
            })
            LottieCompositionFactory.fromJsonInputStream(assets.open("lottie/data.json"), "cacheKey")
                .addListener { result: LottieComposition ->
                    lottieView?.apply {
                        setComposition(result)
                        addAnimatorListener(object : Animator.AnimatorListener {
                            override fun onAnimationStart(animation: Animator) {
                                // 44dp的容器放不下100dp的动画，先缩放方法
                                lottieView?.let {
                                    it.scaleX = scale
                                    it.scaleY = scale
                                }
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                // 动画结束后还原
                                lottieView?.let {
                                    it.scaleX = 1f
                                    it.scaleY = 1f
                                }
                            }

                            override fun onAnimationCancel(animation: Animator) {}

                            override fun onAnimationRepeat(animation: Animator) {}
                        })
                        playAnimation()
                    }
                }
        }
    }
}