package com.colorblind.spectra.UI.slider

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.colorblind.spectra.Adapter.SliderAdapter
import com.colorblind.spectra.R

class SliderActivity : AppCompatActivity() {

    private lateinit var sliderAdapter: SliderAdapter
    private lateinit var dots: ArrayList<TextView>
    private lateinit var layouts: IntArray
    private lateinit var sliderPager: ViewPager
    private lateinit var btnGetStarted: Button
    private lateinit var dotsLayout: LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private var currentPage = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slider)

        statusBarTransparent()

        sliderPager = findViewById(R.id.slider_pager)
        dotsLayout = findViewById(R.id.dotsLayout)
        btnGetStarted = findViewById(R.id.btnext) // Tombol Get Started

        layouts = intArrayOf(
            R.layout.slider1,
            R.layout.slider2,
            R.layout.slider3,
            R.layout.slider4
        )

        sliderAdapter = SliderAdapter(layouts, this)
        sliderPager.adapter = sliderAdapter

        addDotsIndicator(0)

        sliderPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                addDotsIndicator(position)
                currentPage = position

                if (position == layouts.size - 1) {
                    btnGetStarted.visibility = View.VISIBLE
                } else {
                    btnGetStarted.visibility = View.INVISIBLE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
        btnGetStarted.setOnClickListener {
            val intent = Intent(this, com.colorblind.spectra.UI.form.FormActivity::class.java)
            startActivity(intent)
            finish() // Tutup SliderActivity agar tidak bisa balik
        }
        autoSlidePages()
    }

    private fun autoSlidePages() {
        val runnable = object : Runnable {
            override fun run() {
                if (currentPage < layouts.size - 1) {
                    sliderPager.currentItem = currentPage + 1
                }
                handler.postDelayed(this, 3000) // Ganti slide setiap 3 detik
            }
        }
        handler.postDelayed(runnable, 3000)
    }

    private fun addDotsIndicator(position: Int) {
        dots = ArrayList()
        dotsLayout.removeAllViews()

        for (i in layouts.indices) {
            val dot = TextView(this)
            dot.text = "-"
            dot.textSize = 35f
            dot.setTextColor(ContextCompat.getColor(this, R.color.colorTransparentWhite))
            dots.add(dot)
            dotsLayout.addView(dot)
        }

        if (dots.isNotEmpty()) {
            dots[position].setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun statusBarTransparent() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
