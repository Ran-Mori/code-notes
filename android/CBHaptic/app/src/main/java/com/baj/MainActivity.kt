package com.baj

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private var tvClickToVibrate: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvClickToVibrate = findViewById(R.id.click_to_vibrate)
        tvClickToVibrate?.setOnClickListener {
            (getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)?.vibrate(VibrationEffect.createOneShot(300L, 100))
        }

    }

}