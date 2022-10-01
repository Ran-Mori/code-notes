package com.baj

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

        textView = findViewById(R.id.text_vew)

        textView?.setOnClickListener {
            Log.d(TAG, "text view onClick")
        }
    }
}