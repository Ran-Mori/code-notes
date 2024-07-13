package com.gesture_detector

import android.app.Activity
import android.view.GestureDetector
import android.widget.ViewFlipper
import java.util.ArrayList
import android.widget.TextView
import android.os.Vibrator
import android.graphics.Color
import android.view.animation.Animation
import android.os.Bundle
import android.util.Log
import android.view.animation.TranslateAnimation
import android.view.animation.OvershootInterpolator
import android.view.Gravity
import android.view.ViewGroup
import android.view.MotionEvent
import kotlin.math.abs

class FlipperActivity : Activity() {

    companion object {
        private const val SWIPE_MIN_DISTANCE = 100
        private const val SWIPE_MIN_VELOCITY = 100
        private val colors = intArrayOf(
            Color.rgb(255, 128, 128),
            Color.rgb(128, 255, 128),
            Color.rgb(128, 128, 255),
            Color.rgb(128, 128, 128)
        )
    }

    private var flipper: ViewFlipper? = null
    private var views: ArrayList<TextView>? = null
    private var gesturedetector: GestureDetector? = null
    private var vibrator: Vibrator? = null

    private var animleftin = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animleftout = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f,
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animrightin = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animrightout = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f,
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animupin = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
        Animation.RELATIVE_TO_PARENT, +1.0f, Animation.RELATIVE_TO_PARENT, 0.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animupout = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, -1.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animdownin = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
        Animation.RELATIVE_TO_PARENT, -1.0f, Animation.RELATIVE_TO_PARENT, 0.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }
    private val animdownout = TranslateAnimation(
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, 0.0f,
        Animation.RELATIVE_TO_PARENT, 0.0f, Animation.RELATIVE_TO_PARENT, +1.0f
    ).apply {
        duration = 1000
        interpolator = OvershootInterpolator()
    }

    private var isDragMode = false
    private var currentview = 0

    private val gestureListener = object : GestureDetector.OnGestureListener {

        override fun onDown(e: MotionEvent): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnGestureListener onDown call")
            return false
        }

        override fun onFling(
            event1: MotionEvent, event2: MotionEvent,
            velocityX: Float, velocityY: Float
        ): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnGestureListener onFling call")
            if (isDragMode) return false
            val ev1x = event1.x
            val ev1y = event1.y
            val ev2x = event2.x
            val ev2y = event2.y
            val xdiff = abs(ev1x - ev2x)
            val ydiff = abs(ev1y - ev2y)
            val xvelocity = abs(velocityX)
            val yvelocity = abs(velocityY)
            if (xvelocity > SWIPE_MIN_VELOCITY &&
                xdiff > SWIPE_MIN_DISTANCE
            ) {
                if (ev1x > ev2x) { // Swipe left
                    --currentview
                    if (currentview < 0) {
                        currentview = views!!.size - 1
                    }
                    flipper!!.inAnimation = animleftin
                    flipper!!.outAnimation = animleftout
                } else { // Swipe right
                    ++currentview
                    if (currentview >= views!!.size) {
                        currentview = 0
                    }
                    flipper!!.inAnimation = animrightin
                    flipper!!.outAnimation = animrightout
                }
                flipper!!.scrollTo(0, 0)
                flipper!!.displayedChild = currentview
            } else if (yvelocity > SWIPE_MIN_VELOCITY &&
                ydiff > SWIPE_MIN_DISTANCE
            ) {
                if (ev1y > ev2y) { // Swipe up
                    --currentview
                    if (currentview < 0) {
                        currentview = views!!.size - 1
                    }
                    flipper!!.inAnimation = animupin
                    flipper!!.outAnimation = animupout
                } else { // swipe down
                    ++currentview
                    if (currentview >= views!!.size) {
                        currentview = 0
                    }
                    flipper!!.inAnimation = animdownin
                    flipper!!.outAnimation = animdownout
                }
                flipper!!.scrollTo(0, 0)
                flipper!!.displayedChild = currentview
            }
            return false
        }

        override fun onLongPress(e: MotionEvent) {
            Log.d("IzumiSakai", "FlipperActivity OnGestureListener onLongPress call")
            vibrator!!.vibrate(200)
            flipper!!.scrollTo(0, 0)
            isDragMode = !isDragMode
            setViewText()
        }

        override fun onScroll(
            e1: MotionEvent, e2: MotionEvent,
            distanceX: Float, distanceY: Float
        ): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnGestureListener onScroll call")
            if (isDragMode) flipper!!.scrollBy(distanceX.toInt(), distanceY.toInt())
            return false
        }

        override fun onShowPress(e: MotionEvent) {
            Log.d("IzumiSakai", "FlipperActivity OnGestureListener onShowPress call")
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnGestureListener onSingleTapUp call")
            return false
        }
    }

    private val doubleTapListener = object : GestureDetector.OnDoubleTapListener {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnDoubleTapListener onDoubleTap call")
            flipper!!.scrollTo(0, 0)
            return false
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnDoubleTapListener onDoubleTapEvent call")
            return false
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            Log.d("IzumiSakai", "FlipperActivity OnDoubleTapListener onSingleTapConfirmed call")
            return false
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        flipper = ViewFlipper(this)
        gesturedetector = GestureDetector(this, gestureListener)
        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
        gesturedetector?.setOnDoubleTapListener(doubleTapListener)
        flipper?.inAnimation = animleftin
        flipper?.outAnimation = animleftout
        flipper?.flipInterval = 3000
        flipper?.animateFirstView = true
        prepareViews()
        addViews()
        setViewText()
        setContentView(flipper)
    }

    private fun prepareViews() {
        var view: TextView?
        views = ArrayList()
        for (color in colors) {
            view = TextView(this)
            view.setBackgroundColor(color)
            view.setTextColor(Color.BLACK)
            view.gravity = Gravity.CENTER_HORIZONTAL or Gravity.CENTER_VERTICAL
            views?.add(view)
        }
    }

    private fun addViews() {
        for (index in views!!.indices) {
            flipper?.addView(
                views!![index], index,
                ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
        }
    }

    private fun setViewText() {
        val text = getString(if (isDragMode) R.string.app_info_drag else R.string.app_info_flip)
        for (index in views!!.indices) {
            views!![index].text = text
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        Log.d("IzumiSakai", "FlipperActivity onTouchEvent start, action = ${event.action.getMotionEventString()}")
        val result = gesturedetector?.onTouchEvent(event) ?: super.onTouchEvent(event)
        Log.d("IzumiSakai", "FlipperActivity onTouchEvent end, result = ${result}, action = ${event.action.getMotionEventString()}")
        return result
    }
}