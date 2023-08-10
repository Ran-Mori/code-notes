package com.okhttp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private val textView: TextView by lazy { findViewById(R.id.text_view) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView.setOnClickListener {
            val request: Request = Request.Builder()
                .url("https://jsonplaceholder.typicode.com/comments/3")
                .build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.d(TAG, "call = ${call}, e = ${e}")
                }

                override fun onResponse(call: Call, response: Response) {
                    Log.d(TAG, "call = ${call}, response = ${response}")
                }
            })
        }
    }
}