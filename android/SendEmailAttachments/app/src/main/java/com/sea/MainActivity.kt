package com.sea

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.TextView
import java.io.File

class MainActivity : AppCompatActivity() {

    private var button: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.send_email_button)

        Log.d("IzumiSakai", Environment.getStorageDirectory().absolutePath) //result -> /storage
        Log.d("IzumiSakai", Environment.getExternalStorageDirectory().absolutePath) //result -> /storage/emulated/0
        Log.d("IzumiSakai", Environment.getRootDirectory().absolutePath) //result -> /system
        Log.d("IzumiSakai", applicationContext.filesDir.absolutePath) //result -> /data/user/0/com.sea/files

        button?.setOnClickListener {
            val intent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_SUBJECT, "Intent.EXTRA_SUBJECT")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("izumisakai-zy@outlook.com", "izumisakai.zy@gmail.com"))
                putExtra(Intent.EXTRA_TEXT, "Intent.EXTRA_TEXT")
                //把它注释掉，因为'path' 路径会crash
//                putExtra(Intent.EXTRA_STREAM, Uri.fromFile(File("path")))
                type = " text/plain"
            }

            startActivity(intent)
        }
    }
}