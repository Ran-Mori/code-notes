package com.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

class SecondActivity : AppCompatActivity() {

    private val secondText: View by lazy { findViewById(R.id.second_text) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("IzumiSakai", "SecondActivity onCreate")
        setContentView(R.layout.activity_second)
        secondText.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("IzumiSakai", "SecondActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("IzumiSakai", "SecondActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("IzumiSakai", "SecondActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("IzumiSakai", "SecondActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("IzumiSakai", "SecondActivity onDestroy")
    }
}