package com.cbservice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    private var startButton: TextView? = null
    private var stopButton: TextView? = null
    private val serviceIntent by lazy(LazyThreadSafetyMode.NONE) { Intent(this, TrackService::class.java) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 0)
        }

        startButton = findViewById(R.id.start_service)
        stopButton = findViewById(R.id.stop_service)

        startButton?.setOnClickListener {
            startService(serviceIntent)
            Toast.makeText(this, "Starting", Toast.LENGTH_LONG).show()
        }

        stopButton?.setOnClickListener {
            stopService(serviceIntent)
            Toast.makeText(this, "Stopping", Toast.LENGTH_LONG).show()
        }
    }
}