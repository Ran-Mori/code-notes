package com.se.shareelement

import android.app.ActivityOptions
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import com.se.MainActivity
import com.se.R

class ActivityB : AppCompatActivity() {

    private var rootView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)

        rootView = LayoutInflater.from(this).inflate(R.layout.activity_shareelement_b, null)
        setContentView(rootView)

        initShareElement()
    }

    private fun initShareElement() {
        val bounds: Transition = ChangeBounds().apply { duration = 10000 }
        window.sharedElementEnterTransition = bounds
        window.sharedElementReturnTransition = bounds

        val btn = rootView?.findViewById<View>(R.id.bt_click) ?: return
        val iv = rootView?.findViewById<View>(R.id.iv_for_share) ?: return
        btn.setOnClickListener {
            val bundle = ActivityOptions.makeSceneTransitionAnimation(this, iv,"shareElement").toBundle()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent, bundle)
        }
    }
}