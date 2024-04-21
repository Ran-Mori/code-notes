package com.pinch

import android.graphics.Matrix
import android.graphics.PointF
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.sqrt


class MainActivity : AppCompatActivity(), View.OnTouchListener {

    private val TAG = "Touch"

    private var imageView: ImageView? = null

    // These matrices will be used to move and zoom image
    private var matrix: Matrix = Matrix()
    private var savedMatrix: Matrix = Matrix()

    // We can be in one of these 3 states
    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2
    private var mode = NONE

    // Remember some things for zooming
    private var start = PointF()
    private var mid = PointF()
    private var oldDist = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.imageView)

        imageView?.apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setOnTouchListener(this@MainActivity)
        }
    }

    override fun onTouch(view: View?, event: MotionEvent?): Boolean {
        view ?: return false
        event ?: return false

        val image = (view as? ImageView) ?: return false
        image.scaleType = ImageView.ScaleType.MATRIX

        var scale = 0.0F

        when(event.action and MotionEvent.ACTION_MASK) {

            //first finger down only
            MotionEvent.ACTION_DOWN -> {
                savedMatrix.set(matrix)
                start.set(event.x, event.y)
                Log.d(TAG, "mode=DRAG")
                mode = DRAG
            }

            //first finger lifted
            MotionEvent.ACTION_UP,
            //second finger lifted
            MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                Log.d(TAG, "mode=NONE")
            }
            //second finger down
            MotionEvent.ACTION_POINTER_DOWN -> {
                oldDist = spacing(event); // calculates the distance between two points where user touched.
                Log.d(TAG, "oldDist=" + oldDist);
                // minimal distance between both the fingers
                if (oldDist > 5f) {
                    savedMatrix.set(matrix);
                    midPoint(mid, event); // sets the mid-point of the straight line between two points where user touched.
                    mode = ZOOM;
                    Log.d(TAG, "mode=ZOOM" );
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) { //movement of first finger
                    matrix.set(savedMatrix);
                    if (view.left >= -392) {
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                    }
                } else if (mode == ZOOM) { //pinch zooming
                    val newDist = spacing(event);
                    Log.d(TAG, "newDist=$newDist");
                    if (newDist > 5f) {
                        matrix.set(savedMatrix);
                        scale = newDist/oldDist; // XXX may need to play with this value to limit it
                        matrix.postScale(scale, scale, mid.x, mid.y);
                    }
                }
            }
        }

        view.imageMatrix = matrix

        return true
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point[x / 2] = y / 2
    }
}