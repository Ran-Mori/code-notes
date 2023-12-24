package com.se.translation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.ViewGroup
import androidx.transition.Transition
import androidx.transition.TransitionValues

class XYTranslation : Transition() {
    override fun captureStartValues(transitionValues: TransitionValues) {
        transitionValues.values["translationX"] = transitionValues.view.translationX
        transitionValues.values["translationY"] = transitionValues.view.translationY
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        transitionValues.values["translationX"] = transitionValues.view.translationX
        transitionValues.values["translationY"] = transitionValues.view.translationY
    }

    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null
        val startX = startValues.values["translationX"] as Float
        val startY = startValues.values["translationY"] as Float
        val endX = endValues.values["translationX"] as Float
        val endY = endValues.values["translationY"] as Float

        var translationXAnim: Animator? = null
        if (startX != endX) {
            translationXAnim = ObjectAnimator.ofFloat(endValues.view, "translationX", startX, endX)
        }

        var translationYAnim: Animator? = null
        if (startY != endY) {
            translationYAnim = ObjectAnimator.ofFloat(endValues.view, "translationY", startY, endY)
        }

        return mergeAnimators(translationXAnim, translationYAnim)
    }

    private fun mergeAnimators(animator1: Animator?, animator2: Animator?): Animator? {
        return if (animator1 == null) {
            animator2
        } else if (animator2 == null) {
            animator1
        } else {
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(animator1, animator2)
            animatorSet
        }
    }
}