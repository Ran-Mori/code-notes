package whu.viewonpredraw

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout

class MainActivity : AppCompatActivity() {


    private var topFrameLayout: CustomFrameLayout? = null
    private var button: Button? = null
    private var bottomFrameLayout: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initView()
        initClickListener()
    }

    private fun initView() {
        topFrameLayout = findViewById(R.id.top_frame_layout)
        button = findViewById(R.id.my_bottom)
        bottomFrameLayout = findViewById(R.id.bottom_frame_layout)

        topFrameLayout?.setTargetView(bottomFrameLayout)
    }

    private fun initClickListener() {

        button?.setOnClickListener {
            bottomFrameLayout?.layoutParams?.apply {
                height += 100
                bottomFrameLayout?.layoutParams = this
            }
        }
    }
}