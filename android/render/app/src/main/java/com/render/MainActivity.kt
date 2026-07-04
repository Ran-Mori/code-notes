package com.render

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Trace
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import com.render.common.button
import com.render.common.dp
import com.render.common.label
import com.render.common.sectionTitle
import com.render.common.spacer
import com.render.renderthread.RenderThreadLabActivity
import com.render.textureview.TextureViewLabActivity
import com.render.textureupload.TextureUploadLabActivity

class MainActivity : Activity() {

    private val routes = listOf(
        StudyRoute(
            title = "RenderThread 学习",
            description = "观察 Choreographer、View.onDraw、FrameMetrics、SurfaceView 对照组，以及 Perfetto 里的 RenderThread 接手位置。",
            target = RenderThreadLabActivity::class.java
        ),
        StudyRoute(
            title = "大纹理上传",
            description = "用大 Bitmap + 只改 1px 的方式模拟脏纹理反复上传到 GPU，重点观察 RenderThread/HWUI 的上传压力。",
            target = TextureUploadLabActivity::class.java
        ),
        StudyRoute(
            title = "TextureView 学习",
            description = "把 TextureView 拆成 View、SurfaceTexture、Surface、BufferQueue 和 producer/consumer，观察它如何接入普通 View 树。",
            target = TextureViewLabActivity::class.java
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Trace.beginSection("RenderLab.Router#onCreate")
        try {
            setContentView(createContentView())
        } finally {
            Trace.endSection()
        }
    }

    private fun createContentView(): View {
        val root = ScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(Color.rgb(247, 248, 250))
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(28))
        }
        root.addView(
            content,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(label("Render 学习实验台", 26f, Color.rgb(20, 25, 32), Typeface.BOLD))
        content.addView(
            label(
                "选择一个主题进入。每个学习页独立维护自己的实验控件、trace slice、FrameMetrics 和事件日志。",
                15f,
                Color.rgb(70, 77, 88),
                Typeface.NORMAL
            )
        )
        content.addView(spacer(8))

        routes.forEachIndexed { index, route ->
            content.addView(sectionTitle("${index + 1}. ${route.title}"))
            content.addView(label(route.description, 14f, Color.rgb(70, 77, 88), Typeface.NORMAL))
            content.addView(
                button("打开 ${route.title}") {
                    startActivity(Intent(this, route.target))
                }
            )
        }

        return root
    }

    private data class StudyRoute(
        val title: String,
        val description: String,
        val target: Class<out Activity>
    )
}
