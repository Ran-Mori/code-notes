package com.lifecycleowner

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.lifecycleowner.observer.MyLifeCycle

class MainActivity : AppCompatActivity(), LifecycleOwner {

    private var lifecycleRegistry: LifecycleRegistry? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycleRegistry = LifecycleRegistry(this)

        lifecycleRegistry?.currentState = Lifecycle.State.CREATED

        lifecycleRegistry?.addObserver(MyLifeCycle())
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry?.currentState = Lifecycle.State.STARTED
    }

    override fun getLifecycle(): Lifecycle {
        (lifecycleRegistry as? Lifecycle)?.let {
            return it
        }
        return super.getLifecycle()
    }
}