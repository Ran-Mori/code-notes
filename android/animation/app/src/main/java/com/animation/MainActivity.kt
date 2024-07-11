package com.animation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        private const val SHAKE = 1
        private const val ROTATE_XML = 2
        private const val ROTATE_PROGRAMING = 3
    }

    private var textView: TextView? = null
    private var animationType = ROTATE_PROGRAMING

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.text_view)

        when(animationType) {
            SHAKE -> initShake()
            ROTATE_XML -> initRotateXml()
            ROTATE_PROGRAMING -> initRotatePrograming()
        }
    }

    private fun initShake() {
        textView?.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.shake))
        }
    }

    private fun initRotateXml() {
        textView?.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.rotate))
        }
    }

    private fun initRotatePrograming() {
        textView?.setOnClickListener {
            it.clearAnimation()
            it.startAnimation(midToRightRotate)
        }
    }

    private val midToRightRotate = RotateAnimation(
        0f,
        -15f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f
    ).apply {
        duration = 100
        interpolator = LinearInterpolator()
        repeatCount = 0
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {
                Log.d("IzumiSakai", "onAnimationStart")
            }

            override fun onAnimationEnd(animation: Animation?) {
                textView?.startAnimation(rightToLeftRotate)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private val rightToLeftRotate = RotateAnimation(
        -15f,
        15f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f).apply {
        duration = 150
        interpolator = LinearInterpolator()
        repeatCount = 5
        repeatMode = Animation.REVERSE
        setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}

            override fun onAnimationEnd(animation: Animation?) {
                textView?.startAnimation(rightToMiddleRotate)
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    private val rightToMiddleRotate = RotateAnimation(
        -15f,
        0f,
        Animation.RELATIVE_TO_SELF,
        0.5f,
        Animation.RELATIVE_TO_SELF,
        0.5f).apply {
        duration = 100
        interpolator = LinearInterpolator()
    }
}