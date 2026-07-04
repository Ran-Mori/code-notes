package com.render.common

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

fun Activity.label(text: String, sp: Float, color: Int, style: Int): TextView =
    TextView(this).apply {
        this.text = text
        textSize = sp
        setTextColor(color)
        typeface = Typeface.create(Typeface.DEFAULT, style)
        includeFontPadding = true
        setLineSpacing(0f, 1.12f)
    }

fun Activity.sectionTitle(text: String): TextView =
    label(text, 18f, Color.rgb(20, 25, 32), Typeface.BOLD).apply {
        setPadding(0, dp(18), 0, dp(4))
    }

fun Activity.codeBlock(text: String): TextView =
    TextView(this).apply {
        this.text = text
        textSize = 12.5f
        setTextColor(Color.rgb(37, 43, 52))
        typeface = Typeface.MONOSPACE
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = rounded(Color.rgb(236, 240, 245), 12f, Color.rgb(220, 226, 234), 1)
        setLineSpacing(0f, 1.08f)
    }

fun Activity.switchRow(
    text: String,
    checked: Boolean,
    onChanged: (CompoundButton, Boolean) -> Unit
): Switch =
    Switch(this).apply {
        this.text = text
        textSize = 14.5f
        setTextColor(Color.rgb(45, 51, 62))
        isChecked = checked
        setPadding(0, dp(6), 0, dp(6))
        setOnCheckedChangeListener(onChanged)
    }

fun Activity.button(text: String, onClick: () -> Unit): Button =
    Button(this).apply {
        this.text = text
        textSize = 13f
        isAllCaps = false
        setOnClickListener { onClick() }
    }

fun Activity.buttonRow(vararg buttons: Button): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        for (button in buttons) {
            addView(
                button,
                LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(8)
                }
            )
        }
    }

fun Activity.spacer(heightDp: Int): View =
    View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            dp(heightDp)
        )
    }

fun Activity.rounded(fill: Int, radiusDp: Float, stroke: Int, strokeDp: Int): GradientDrawable =
    GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeDp > 0) {
            setStroke(dp(strokeDp), stroke)
        }
    }

fun Activity.dp(value: Int): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()

fun Activity.dp(value: Float): Int =
    (value * resources.displayMetrics.density + 0.5f).toInt()
