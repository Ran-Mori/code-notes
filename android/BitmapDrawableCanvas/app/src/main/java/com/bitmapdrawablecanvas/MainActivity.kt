package com.bitmapdrawablecanvas

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "IzumiSakai"
    }

    private var imageView: ImageView? = null
    private var textView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.image_vew)
        textView = findViewById(R.id.text_view)

        imageView?.setOnClickListener {
            // Bitmap -> Drawable
//            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.takagi)
//            val bitmapDrawable = BitmapDrawable(resources, bitmap)
//            imageView?.setImageDrawable(bitmapDrawable)

            // Drawable -> Bitmap
//            val drawable = BitmapDrawable(resources, BitmapFactory.decodeResource(resources, R.drawable.takagi))
//            drawable.setBounds(200, 200, 1000 ,1000)
//            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(bitmap)
//            drawable.draw(canvas)
//            imageView?.setImageDrawable(BitmapDrawable(resources, bitmap))

            Log.d(TAG, "image view onClick")
        }

        textView?.setOnClickListener {
            // View -> Bitmap
//            val bitmap = Bitmap.createBitmap(textView?.width ?: 100, textView?.height ?: 100, Bitmap.Config.ARGB_8888)
//            val canvas = Canvas(bitmap)
//            textView?.draw(canvas)

            Log.d(TAG, "text view onClick")
        }
    }
}