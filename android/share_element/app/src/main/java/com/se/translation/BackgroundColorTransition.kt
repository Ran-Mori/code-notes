package com.se.translation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.graphics.drawable.ColorDrawable
import android.view.ViewGroup
import androidx.transition.Transition
import androidx.transition.TransitionValues

class BackgroundColorTransition : Transition() {

    override fun captureStartValues(transitionValues: TransitionValues) {
        val drawable = transitionValues.view.background as? ColorDrawable ?: return
        transitionValues.values["backgroundColor"] = drawable.color
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        val drawable = transitionValues.view.background as? ColorDrawable ?: return
        transitionValues.values["backgroundColor"] = drawable.color
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null
        val startColor = (startValues.values["backgroundColor"] as? Int) ?: return null
        val endColor = (endValues.values["backgroundColor"] as? Int) ?: return null
        if (startColor != endColor) {
            return ObjectAnimator.ofArgb(endValues.view, "backgroundColor", startColor, endColor)
        }
        return super.createAnimator(sceneRoot, startValues, endValues)
    }
}