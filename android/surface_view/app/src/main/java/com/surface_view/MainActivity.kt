package com.surface_view

import android.opengl.GLSurfaceView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.surface_view.gl.MyOpenGLRender
import com.surface_view.vv.MyVideoView

class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
        const val NORMAL = "normal"
        const val GL = "gl"
        const val VIDEO_VIEW = "video_view"
    }

    private var type = GL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when(type) {
            NORMAL -> {
                setContentView(R.layout.activity_main)
            }
            GL -> {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
                val surfaceView = GLSurfaceView(this).also { it.setRenderer(MyOpenGLRender()) }
                setContentView(surfaceView)
            }
            VIDEO_VIEW -> {
                setContentView(MyVideoView(this))
            }
        }
    }
}