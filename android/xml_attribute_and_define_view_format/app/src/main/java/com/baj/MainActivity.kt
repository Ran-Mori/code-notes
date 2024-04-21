package com.baj

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var selfTextView: SelfTextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        selfTextView = findViewById(R.id.self_text_vew)

        selfTextView?.setOnClickListener {
            selfTextView?.toastSelfDefineText()
        }
    }
}