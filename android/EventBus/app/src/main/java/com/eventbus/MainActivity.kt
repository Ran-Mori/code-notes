package com.eventbus

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.eventbus.event.EventBusMessage
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var textView: TextView? = null
    private var button: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //注册
        EventBus.getDefault().register(this)

        val view = LayoutInflater.from(this).inflate(R.layout.activity_main, null)
        setContentView(view)

        textView = view.findViewById(R.id.text_view)
        button = view.findViewById(R.id.button)

        textView?.setOnClickListener(this)
        button?.setOnClickListener(this)
    }


    override fun onDestroy() {
        super.onDestroy()

        //解注册
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun handleMessage(msg: EventBusMessage) {
        textView?.text = msg.msg
    }

    override fun onClick(view: View?) {
        view ?: return
        when (view.id) {
            R.id.text_view -> {

            }

            R.id.button -> {
                EventBus.getDefault().post(EventBusMessage("post a event bug message"))
            }
        }
    }
}