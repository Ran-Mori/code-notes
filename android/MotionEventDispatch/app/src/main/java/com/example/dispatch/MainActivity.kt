package com.example.dispatch

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.TextView
import com.example.dispatch.Single.getMotionEventString

class MainActivity : AppCompatActivity() {

    companion object {
        // 用于测试事件整体分发
        private const val ONE = 1
        // 用于测试点击事件中途抬起变cancel
        private const val TWO = 2
    }

    private val scene = TWO

    private val textView: TextView by lazy { findViewById(R.id.text_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runBlockDiffScene({
            setContentView(R.layout.activity_main)
        }, {
            setContentView(R.layout.activity_second)
            textView.setOnClickListener {
                Log.d("IzumiSakai", "on click")
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        var result = false
        runBlockDiffScene({
            Log.e(Single.DISPATCH_LOG_TAG, "MainActivity.dispatchTouchEvent call start, action: ${ev?.action?.getMotionEventString()}")
            result = super.dispatchTouchEvent(ev)
            Log.e(Single.DISPATCH_LOG_TAG, "MainActivity.dispatchTouchEvent call end, result: $result, action: ${ev?.action?.getMotionEventString()}")
        }, {
            result = super.dispatchTouchEvent(ev)
        })
        return result
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        var result = false
        runBlockDiffScene({
            Log.e(Single.DISPATCH_LOG_TAG, "MainActivity.onTouchEvent call start, action: ${event?.action?.getMotionEventString()}")
            result = super.onTouchEvent(event)
            Log.e(Single.DISPATCH_LOG_TAG, "MainActivity.onTouchEvent call end, result: $result, action: ${event?.action?.getMotionEventString()}")

        }, {
            result = super.onTouchEvent(event)
        })
        return result
    }

    private fun runBlockDiffScene(oneBlock: () -> Unit, twoBlock: () -> Unit) {
        if (scene == ONE) {
            oneBlock.invoke()
        } else if (scene == TWO) {
            twoBlock.invoke()
        }
    }
}