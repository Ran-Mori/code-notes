package com.binder.douyin

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.binder.R

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var textView: TextView? = null
    private val client: CalculatorClient by lazy { CalculatorClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.text_view)
        client.connect(this)

        textView?.setOnClickListener {
            Log.d(TAG, "text view onClick, call add by binder value = ${client.add(3,6)}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect(this)
    }
}