package com.cbwebview

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JSB原来就是这样搞的
 */
class WebInterface(private val mContext: Context) {

    @JavascriptInterface
    fun showToast(toast: String) {
        Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show()
    }
}