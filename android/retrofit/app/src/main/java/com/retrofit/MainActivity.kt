package com.retrofit

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private val textView: TextView by lazy { findViewById(R.id.text_view) }
    private val retrofitClient by lazy { RetrofitUtil.getClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView.setOnClickListener {
//            retrofitClient.getRespById(3)
//            retrofitClient.getAllResp()
            retrofitClient.getComment(3)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                        Log.d(TAG, "result = ${it}")
                    }, {
                        Log.d(TAG, "on Error")
                    }
                )
        }
    }
}