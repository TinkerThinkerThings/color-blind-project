package com.colorblind.spectra.UI.slider

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
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
    private lateinit var btnNext: Button
    private lateinit var btnGetStarted: Button
    private lateinit var dotsLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slider)

        statusBarTransparent()

        sliderPager = findViewById(R.id.slider_pager)
        dotsLayout = findViewById(R.id.dotsLayout)
        btnNext = findViewById(R.id.bstnext)          // Tombol Next
        btnGetStarted = findViewById(R.id.btnext)     // Tombol Get Started

        layouts = intArrayOf(
            R.layout.slider1,
            R.layout.slider2,
            R.layout.slider3,
            R.layout.slider4
        )

        sliderAdapter = SliderAdapter(layouts, this)
        sliderPager.adapter = sliderAdapter

        addDotsIndicator(0)

        btnNext.setOnClickListener {
            val currentItem = sliderPager.currentItem
            if (currentItem < layouts.size - 1) {
                sliderPager.currentItem = currentItem + 1
            }
        }

//        btnGetStarted.setOnClickListener {
//            val intent = Intent(this, LoginActivity::class.java)
//            startActivity(intent)
//            finish()
//        }

        sliderPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                addDotsIndicator(position)

                if (position == layouts.size - 1) {
                    // Di slide terakhir: tampilkan tombol Get Started, sembunyikan Next
                    btnGetStarted.visibility = View.VISIBLE
                    btnNext.visibility = View.INVISIBLE
                } else {
                    // Slide selain terakhir: tampilkan Next, sembunyikan Get Started
                    btnGetStarted.visibility = View.INVISIBLE
                    btnNext.visibility = View.VISIBLE
                }
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })
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
}
