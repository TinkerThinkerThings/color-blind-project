package com.colorblind.spectra.UI.menu

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.colorblind.spectra.R

class PrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_privacy)

        // Hilangkan ActionBar default
        supportActionBar?.hide()

        // Back button
        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // Safe area biar gak ketimpa status bar/navigation bar
        val rootView = findViewById<android.view.View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Atur text kebijakan privasi (pakai HTML parsing biar <b>, <i>, <u> jalan)
        val textPrivacy: TextView = findViewById(R.id.textPrivacy)
        textPrivacy.text = HtmlCompat.fromHtml(
            getString(R.string.privacy_text),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
    }
}
