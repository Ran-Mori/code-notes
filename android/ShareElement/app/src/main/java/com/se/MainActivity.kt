package com.se

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.se.fragment.BigImageFragment
import com.se.translation.BackgroundColorTransition
import com.se.translation.XYTranslation

class MainActivity : AppCompatActivity() {

    companion object {
        const val TRANSITION_NAME = "transitionName"
    }

    private var rootView: View? = null
    private var strategy: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = if (strategy == 0) {
            R.layout.activity_fragment
        } else {
            R.layout.activity_transition
        }
        rootView = LayoutInflater.from(this).inflate(layoutId, null)
        setContentView(rootView)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (strategy == 0) {
            initFragment()
        } else {
            initTransition()
        }
    }

    private fun initFragment() {
        supportFragmentManager
            .beginTransaction()
            .add(R.id.fragment_container, BigImageFragment.newInstance(), BigImageFragment.TAG)
            .commitAllowingStateLoss()
    }

    @SuppressLint("CutPasteId")
    private fun initTransition() {
        val backgroundColor1 = Color.parseColor("#ff0000")
        val backgroundColor2 = Color.parseColor("#00ff00")
        var index = 0

        rootView?.findViewById<View>(R.id.btClick)?.setOnClickListener {
            val viewGroup = rootView?.findViewById<ViewGroup>(R.id.beginDelayRoot) ?: return@setOnClickListener
            val view1 = viewGroup.getChildAt(0) ?: return@setOnClickListener
            val view2 = viewGroup.getChildAt(1) ?: return@setOnClickListener


            view1.translationX = 100f
            view2.translationX = 100f

            TransitionManager.beginDelayedTransition(viewGroup, TransitionSet().apply {
                addTransition(ChangeBounds())
                addTransition(XYTranslation())
                addTransition(BackgroundColorTransition())
                duration = 20000
            })

            view1.translationX = 0f
            view2.translationX = 0f

            viewGroup.apply {
                removeView(view1)
                removeView(view2)
                addView(view1)
                addView(view2)
                setBackgroundColor(if (index % 2 == 0) backgroundColor2 else backgroundColor1)
            }
            index++
        }
    }

}