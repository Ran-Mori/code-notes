package com.rxjava

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import java.lang.RuntimeException

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.text_view)

        textView?.setOnClickListener {
            testFlatMap()
        }
    }

    private fun testFlatMap() {
        Observable.create<String> {
            Log.d(TAG, "create thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
            Thread.sleep(2000L)
            throw RuntimeException("") // 验证抛异常会被下面onErrorReturn给拦住
            it.onNext("create onNext")
        }
            .map {
                Log.d(TAG, "map thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
                throw RuntimeException("") // 验证抛异常会被下面onErrorReturn给拦住
                "${it}map"
            }
            .onErrorReturn {
                Log.d(TAG, "onErrorReturn thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
                "error"
            }
            .flatMap { result ->
                Observable.create<String> {
                    Log.d(TAG, "flatMap thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
                    throw RuntimeException("") // 验证抛异常会被下面subscribe()的onError拦住
                    it.onNext("createSecond + ${result}")
                    it.onComplete()
                }
            }
            .subscribeOn(Schedulers.io()) // Observable.create<String>的方法块都会在io线程执行
            .observeOn(AndroidSchedulers.mainThread()) // 下面所有的Observer都在main thread
            .subscribe(
                {
                    Log.d(TAG, "subscribe onNext thread = ${Thread.currentThread().name}, result = $it, time = ${System.currentTimeMillis()}")
                },
                {
                    Log.d(TAG, "subscribe onError thread = ${Thread.currentThread().name}, time = ${System.currentTimeMillis()}")
                    it.printStackTrace()
                }
            )
    }
}