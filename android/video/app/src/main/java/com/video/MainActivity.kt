package com.video

import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var videoView: VideoView? = null
    private val controller by lazy {
        MediaController(this).apply {
            setAnchorView(videoView)
        }
    }
    private val videoPath by lazy { "android.resource://${packageName}/${R.raw.beauty}" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.video_vew)

        videoView?.setOnClickListener {
            (it as? VideoView)?.apply {
                setMediaController(controller)
                setVideoURI(Uri.parse(videoPath))
                requestFocus()
                start()
            }
        }
    }
}