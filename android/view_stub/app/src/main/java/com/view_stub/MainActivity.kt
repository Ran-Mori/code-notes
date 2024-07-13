package com.view_stub

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.ViewStub
import androidx.core.view.get

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var viewStub: ViewStub? = null
    private var flContainer: ViewGroup? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewStub = findViewById(R.id.stub_view)
        viewStub?.apply {
            layoutResource = R.layout.stub_view_layout
            inflate()
        }
        flContainer = findViewById(R.id.fl_container)
        Log.d(TAG, "fl_container's hashCode is ${flContainer?.hashCode()}")
        Log.d(TAG, "fl_container's child id is ${flContainer?.get(0)?.id}")
    }
}