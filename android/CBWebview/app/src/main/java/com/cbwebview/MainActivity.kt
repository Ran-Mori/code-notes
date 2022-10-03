package com.cbwebview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebView

class MainActivity : AppCompatActivity() {

    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this)
        setContentView(webView)

        webView?.apply {
            settings.javaScriptEnabled = true
            addJavascriptInterface(WebInterface(this@MainActivity), "WebInterface")
            //'file:///android_asset'的方式只适用于WebView，其他地方不能这样用
            loadUrl("file:///android_asset/index.html")
        }
    }
}