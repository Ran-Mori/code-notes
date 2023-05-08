package com.coroutines

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import kotlinx.coroutines.*
import kotlin.coroutines.EmptyCoroutineContext

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private val textView: TextView by lazy { findViewById(R.id.text_view) }
    private val scope by lazy { CoroutineScope(EmptyCoroutineContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView.setOnClickListener {
            runBlocking {
                testBasic()
//                testAsync()
//                testTwoCoroutines()
//                testWithContext()
//                testGlobalScope()
            }
        }
    }

    private suspend fun testBasic() {
        scope.launch(Dispatchers.IO) { // launch a new coroutine and continue
            delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
            Log.d(TAG, "World!") // print after delay
        }
        Log.d(TAG, "Hello") // main coroutine continues while a previous one is delayed
    }

    private suspend fun testTwoCoroutines() {
        scope.launch {
            delay(2000L)
            Log.d(TAG,"World 2")
        }
        scope.launch {
            delay(1000L)
            Log.d(TAG,"World 1")
        }
        Log.d(TAG,"Hello")
    }

    private suspend fun testAsync() {
        val deferred: Deferred<Int> = scope.async(Dispatchers.IO) {
            Log.d(TAG, "loading..., timeStamp = ${System.currentTimeMillis()}")
            delay(1000L)
            Log.d(TAG, "loaded!")
            100
        }
        Log.d(TAG, "waiting..., timeStamp = ${System.currentTimeMillis()}")
        Log.d(TAG, "result = ${deferred.await()}")
    }

    private suspend fun testWithContext() {
        scope.launch(Dispatchers.IO) {
            val result = tryToGetData()
            withContext(Dispatchers.Main) {
                Log.d(TAG, "result = ${result}, currentThread is ${Thread.currentThread().name}")
            }
        }
    }

    private suspend fun tryToGetData(): Int {
        delay(1000L)
        Log.d(TAG, "success to get data, currentThread is ${Thread.currentThread().name}")
        return 100
    }

    private suspend fun testGlobalScope() {
        val job = scope.launch {
            GlobalScope.launch(Dispatchers.IO) {
                delay(1000L)
                Log.d(TAG, "GlobalScope.launch finish")
            }
            launch(Dispatchers.IO) {
                delay(1000L)
                Log.d(TAG, "inherit scope launch finish")
            }
            Log.d(TAG, "waiting...")
        }
        delay(500)
        job.cancel()
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}