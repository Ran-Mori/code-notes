package com.baj

import android.content.Context
import android.util.AttributeSet
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView

class SelfTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): AppCompatTextView(context, attrs, defStyle) {
    private var selfDefineText: String? = null

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.SelfTextView)
        selfDefineText = typedArray.getString(R.styleable.SelfTextView_self_define_text)
        typedArray.recycle()
    }

    fun toastSelfDefineText() {
        Toast.makeText(context, selfDefineText, Toast.LENGTH_SHORT).show()
    }
}