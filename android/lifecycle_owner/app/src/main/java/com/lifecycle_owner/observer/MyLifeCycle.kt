package com.lifecycle_owner.observer

import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class MyLifeCycle: LifecycleObserver {

    companion object {
        private const val LOG_TAG = "IzumiSakai"
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        Log.d(LOG_TAG, "on start")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        Log.d(LOG_TAG, "on stop")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        Log.d(LOG_TAG, "on destroy")
    }
}