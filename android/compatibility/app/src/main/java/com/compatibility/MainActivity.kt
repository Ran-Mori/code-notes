package com.compatibility

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.text_view)

        textView?.setOnClickListener {
            Log.d(TAG, "text view onClick")
            Log.d(TAG, "brand = ${Build.BRAND}, sdk_int = ${Build.VERSION.SDK_INT}")
        }
    }
}