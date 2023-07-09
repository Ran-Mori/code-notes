package com.activity

import android.content.Intent
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
        Log.d("IzumiSakai", "MainActivity onCreate")
        savedInstanceState?.let {
            Log.d("IzumiSakai", it.getString("IzumiSakai") ?: "")
        }
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.text_view)

        textView?.setOnClickListener {
//            startActivity(Intent(this, SecondActivity::class.java))

            // 测试singleTop
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d("IzumiSakai", "MainActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d("IzumiSakai", "MainActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d("IzumiSakai", "MainActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("IzumiSakai", "MainActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("IzumiSakai", "MainActivity onDestroy")
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("IzumiSakai", "MainActivity onNewIntent")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("IzumiSakai", "测试onSaveInstanceState成功")
    }
}