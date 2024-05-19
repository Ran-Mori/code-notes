package com.baj

import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.text_view)

        textView?.translationY =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 300F, applicationContext.resources.displayMetrics)

        var i = 0

        textView?.setOnClickListener {
//            it.invalidate()
//            it.postInvalidate()
//            it.requestLayout()
            if (i <= 5) {
                it.scrollBy(0, -20)
                i++
            } else {
                it.requestLayout()
            }

            printCoordinate(it)
        }
    }

    private fun printCoordinate(view: View?) {
        view ?: return
        Log.d(TAG, "left = ${view.left}")
        Log.d(TAG, "right = ${view.right}")
        Log.d(TAG, "top = ${view.top}")
        Log.d(TAG, "bottom = ${view.bottom}")
        Log.d(TAG, "elevation = ${view.elevation}")
        Log.d(TAG, "translationX = ${view.translationX}")
        Log.d(TAG, "translationY = ${view.translationY}")
        Log.d(TAG, "translationZ = ${view.translationZ}")
        Log.d(TAG, "x = ${view.x}")
        Log.d(TAG, "y = ${view.y}")
        Log.d(TAG, "z = ${view.z}")
        Log.d(TAG, "mScrollX = ${view.scrollX}")
        Log.d(TAG, "mScrollY = ${view.scrollY}")
    }
}