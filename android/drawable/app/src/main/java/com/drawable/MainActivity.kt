package com.drawable

import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    companion object {
        const val ANIMATION_DRAWABLE = "animation_drawable"
        const val BASSEL_SHADOW = "bassel_shadow"
        const val LINEAR_GRADIENT_SHADOW = "linear_gradient_shadow"
        const val NOW_BACKGROUND = "now_background"
    }

    private var imageView: ImageView? = null
    private var type = NOW_BACKGROUND

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = findViewById(R.id.image_view)
        when(type) {
            ANIMATION_DRAWABLE -> {
                val animationDrawable = ContextCompat.getDrawable(this, R.drawable.animate_drawable)
                imageView?.background = animationDrawable
                (animationDrawable as? AnimationDrawable)?.start()
            }
            BASSEL_SHADOW -> {
                imageView?.background = ContextCompat.getDrawable(this, R.drawable.bessel_shadow)
            }
            LINEAR_GRADIENT_SHADOW -> {
                imageView?.background = ContextCompat.getDrawable(this, R.drawable.linear_gradient_shadow)
            }
            NOW_BACKGROUND -> {
                imageView?.background = ContextCompat.getDrawable(this, R.drawable.now_background)
            }
        }
    }
}