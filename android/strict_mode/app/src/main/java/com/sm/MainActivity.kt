package com.sm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.StrictMode

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //判断api version
//        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN)

        //用于更方便的检测出ANR
        StrictMode.enableDefaults()

    }
}