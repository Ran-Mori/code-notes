package com.baj

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var textView: TextView? = null

    private val textWatcher by lazy(LazyThreadSafetyMode.NONE) {
        object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d(TAG, "beforeTextChanged -> s = $s, start = $start, count=$count, after=$after")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d(TAG, "onTextChanged -> s = $s, start = $start, before=$before, count=$count")
            }

            override fun afterTextChanged(s: Editable?) {
                Log.d(TAG, "s = $s")
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.text_view)

        textView?.apply {
            addTextChangedListener(textWatcher)
            setOnClickListener {
                text = "${text}1"
            }
        }
    }



}