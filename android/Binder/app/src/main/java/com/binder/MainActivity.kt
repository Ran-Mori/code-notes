package com.binder

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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

        textView?.setOnClickListener {
            Log.d(TAG, "text view onClick, call add by binder value = ${client.add(3,6)}")
        }

        client.connect(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        client.disconnect(this)
    }
}