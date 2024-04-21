package com.se

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.transition.ChangeBounds
import android.transition.Transition
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.se.fragment.BigImageFragment
import com.se.shareelement.ActivityB
import com.se.translation.BackgroundColorTransition
import com.se.translation.XYTranslation


class MainActivity : AppCompatActivity() {

    companion object {
        const val TRANSITION_NAME = "transitionName"
        private const val FRAGMENT = 0
        private const val TRANSITION = 1
        private const val OVERLAY = 2
        private const val SHARE_ELEMENT = 3
    }

    private var rootView: ViewGroup? = null
    private var strategy: Int = SHARE_ELEMENT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_CONTENT_TRANSITIONS)

        val layoutId = when(strategy) {
            FRAGMENT -> R.layout.activity_fragment
            TRANSITION -> R.layout.activity_transition
            OVERLAY -> R.layout.activity_overlay
            SHARE_ELEMENT -> R.layout.activity_shareelement_a
            else -> 0
        }

        rootView = LayoutInflater.from(this).inflate(layoutId, null) as? ViewGroup
        setContentView(rootView)

        when(strategy) {
            FRAGMENT -> initFragment()
            TRANSITION -> initTransition()
            OVERLAY -> initOverlay()
            SHARE_ELEMENT -> initShareElement()
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
        val btn = rootView?.findViewById<View>(R.id.bt_click) ?: return
        val beginDelayRoot = rootView?.findViewById<ViewGroup>(R.id.beginDelayRoot) ?: return
        val backgroundColor1 = Color.parseColor("#ff0000")
        val backgroundColor2 = Color.parseColor("#00ff00")
        var index = 0

        btn.setOnClickListener {
            val view1 = beginDelayRoot.getChildAt(0) ?: return@setOnClickListener
            val view2 = beginDelayRoot.getChildAt(1) ?: return@setOnClickListener

            view1.translationX = 100f
            view2.translationX = 100f

            // 记录开始帧，translationX = 100
            TransitionManager.beginDelayedTransition(beginDelayRoot, TransitionSet().apply {
                addTransition(androidx.transition.ChangeBounds())
                addTransition(XYTranslation())
                addTransition(BackgroundColorTransition())
                duration = 20000
            })

            view1.translationX = 0f
            view2.translationX = 0f

            beginDelayRoot.apply {
                removeView(view1)
                removeView(view2)
                addView(view1)
                addView(view2)
                setBackgroundColor(if (index % 2 == 0) backgroundColor2 else backgroundColor1)
            }
            // 执行到onDraw时记录结束帧，translationX = 0，上两view被移除又添加
            index++
        }
    }

    private fun initOverlay() {
        val ll1 = rootView?.findViewById<LinearLayout>(R.id.ll1_overlay) ?: return
        val ll2 = rootView?.findViewById<LinearLayout>(R.id.ll2_overlay) ?: return
        val btn = rootView?.findViewById<View>(R.id.bt_click) ?: return

        val view = View(this)
        view.layoutParams = LinearLayout.LayoutParams(200, 200)
        view.setBackgroundColor(Color.parseColor("#0000FF"))
        // 需要手动调用layout，不然view显示不出来
        view.layout(0, 0, 200, 200)
        // View的Overlay直接添加View进行绘制
        ll1.overlay?.add(view)

        ll2.apply {
            post {
                val height = measuredHeight
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_launcher_background)?.let {
                    // 需要手动调用setBounds，不然drawable显示不出来
                    it.setBounds(height / 2, 0, height, height / 2)
                    // ViewGroup的Overlay要添加Drawable进行绘制
                    overlay.add(it)
                }
            }
        }

        btn.setOnClickListener {
            // 测试一下OverlayView的动画
            rootView?.let { TransitionManager.beginDelayedTransition(it, XYTranslation()) }
            ll1.translationX += 100
            ll2.translationX += 100
        }
    }

    private fun initShareElement() {
        window.sharedElementExitTransition = null

        val btn = rootView?.findViewById<View>(R.id.bt_click) ?: return
        val iv = rootView?.findViewById<View>(R.id.iv_for_share_a) ?: return
        btn.setOnClickListener {
            val bundle = ActivityOptions.makeSceneTransitionAnimation(this, iv,"shareElement").toBundle()
            val intent = Intent(this, ActivityB::class.java)
            startActivity(intent, bundle)
        }
    }
}