package whu.viewonpredraw

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

class CustomFrameLayout : FrameLayout {

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private var mTargetView: View? = null

    fun setTargetView(targetView: View?) {

        mTargetView = targetView

        targetView?.viewTreeObserver?.addOnPreDrawListener {
            if (!targetView.viewTreeObserver.isAlive) return@addOnPreDrawListener true

            val targetWidth = targetView.measuredWidth
            val targetHeight = targetView.measuredHeight

            val maskViewWidth = width
            val maskViewHeight = height

            if (targetWidth != maskViewWidth || targetHeight != maskViewHeight) {
                layoutParams.apply {
                    width = targetWidth
                    height = targetHeight
                    layoutParams = this
                }
            }
            return@addOnPreDrawListener true
        }
    }
}