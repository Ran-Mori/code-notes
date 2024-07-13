package com.surface_view.vv

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.MediaController
import android.widget.VideoView
import com.surface_view.R

class MyVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : VideoView(context, attrs, defStyle) {

    init {
        val controller by lazy {
            MediaController(context).apply {
                setAnchorView(this)
            }
        }

        val videoPath by lazy { "android.resource://${context.packageName}/${R.raw.beauty}" }

        setOnClickListener {
            (it as? VideoView)?.apply {
                setMediaController(controller)
                setVideoURI(Uri.parse(videoPath))
                requestFocus()
                start()
            }
        }
    }
}