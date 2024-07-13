package com.live_data

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.lifecycle.MutableLiveData

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var liveData = MutableLiveData("init livedata")
    private var textView: TextView? = null
    private var button: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val view = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(view)

        textView = view.findViewById(R.id.text_view)
        button = view.findViewById(R.id.button)

        textView?.setOnClickListener(this)
        button?.setOnClickListener(this)

        liveData.observe(this) {
            textView?.text = it
        }
    }

    override fun onClick(view: View?) {
        view ?: return

        when(view.id) {
            R.id.button -> {
                liveData.value = "点了一下button"
            }
        }
    }
}