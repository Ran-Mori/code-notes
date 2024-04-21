package com.scroll

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var tenTextView: TextView
    private lateinit var oneTextView: TextView
    private lateinit var twoTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootView = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(rootView)

        scrollView = rootView.findViewById(R.id.scrollView)
        tenTextView = rootView.findViewById(R.id.ten_textview)
        oneTextView = rootView.findViewById(R.id.one_textview)
        twoTextView = rootView.findViewById(R.id.two_textview)


        tenTextView.setOnClickListener {
            Log.d("IzumiSakai", "before:mScrollX=${scrollView.scrollY}")
            scrollView.scrollTo(0, 200)
            Log.d("IzumiSakai", "after:mScrollX=${scrollView.scrollY}")
        }

        oneTextView.setOnClickListener {
            Log.d("IzumiSakai", "before:mScrollX=${scrollView.scrollY}")
            scrollView.scrollBy(0, 200)
            Log.d("IzumiSakai", "after:mScrollX=${scrollView.scrollY}")
        }

        twoTextView.setOnClickListener {
            Log.d("IzumiSakai", "before:mScrollX=${scrollView.scrollY}")
            scrollView.scrollTo(0, 0)
            Log.d("IzumiSakai", "after:mScrollX=${scrollView.scrollY}")
        }

        scrollView.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            run {
                Log.d("IzumiSakai", "v=$v")
                Log.d("IzumiSakai", "scrollX=$scrollX")
                Log.d("IzumiSakai", "scrollY=$scrollY")
                Log.d("IzumiSakai", "oldScrollX=$oldScrollX")
                Log.d("IzumiSakai", "oldScrollY=$oldScrollY")
            }
        }
    }
}