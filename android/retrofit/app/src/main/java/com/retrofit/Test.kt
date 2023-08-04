package com.retrofit

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.util.function.Function

class Test(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): View(context, attrs, defStyle) {
    private fun doSomething(block: (String) -> Int): Int {
        return block.invoke("")
    }

    private fun trySetClick() {
        setOnClickListener {
            val a: Function
        }
    }

}