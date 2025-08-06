package com.colorblind.spectra.UI.quiz

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.R
import com.colorblind.spectra.UI.menu.MenuOptionActivity
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IshiharaActivity : AppCompatActivity() {

    private lateinit var textPlateNumber: TextView
    private lateinit var imagePlate: ImageView
    private lateinit var editAnswer: EditText
    private lateinit var buttonNext: MaterialButton
    private lateinit var progressBar: ProgressBar

    private val questions = listOf(
        R.drawable.plate1,
        R.drawable.plate2,
        R.drawable.plate5,
        R.drawable.plate7,
        R.drawable.plate10,
        R.drawable.plate16,
        R.drawable.plate18,
        R.drawable.plate22,
        R.drawable.plate23,
        R.drawable.plate24,
        R.drawable.plate25
    )

    private val answerKeys = listOf("12", "8", "57", "3", "2", "16", "5", "26", "42", "35", "96")

    private var currentIndex = 0
    private val answers = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ishihara)

        // Inisialisasi view
        textPlateNumber = findViewById(R.id.textPlateNumber)
        imagePlate = findViewById(R.id.imagePlate)
        editAnswer = findViewById(R.id.editAnswer)
        buttonNext = findViewById(R.id.buttonNext)
        progressBar = findViewById(R.id.progressBar)

        // Tampilkan pertanyaan pertama
        showQuestion(currentIndex)

        buttonNext.setOnClickListener {
            val answer = editAnswer.text.toString().trim()

            if (answer.isEmpty()) {
                editAnswer.error = "Jawaban tidak boleh kosong"
                return@setOnClickListener
            }

            // Simpan jawaban
            answers.add(answer)

            if (currentIndex < questions.lastIndex) {
                // Maju ke soal berikutnya
                currentIndex++
                showLoadingThenNextQuestion()
            } else {
                // Selesai → proses hasil
                processResult()
            }
        }
    }

    private fun showQuestion(index: Int) {
        textPlateNumber.text = "Plate ${index + 1}"
        imagePlate.setImageResource(questions[index])
        editAnswer.text.clear()
        editAnswer.requestFocus()
    }

    private fun showLoadingThenNextQuestion() {
        progressBar.visibility = View.VISIBLE
        buttonNext.isEnabled = false

        lifecycleScope.launch {
            delay(400)
            progressBar.visibility = View.GONE
            buttonNext.isEnabled = true
            showQuestion(currentIndex)
        }
    }

    private fun processResult() {
        val correctCount = answers.zip(answerKeys).count { (user, key) -> user == key }
        val resultType = getColorBlindnessType(correctCount)

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val latest = db.biodataDao().getLatest()

            if (latest != null) {
                val updated = latest.copy(
                    isIshiharaDone = true,
                    score = correctCount,
                    hasilTes = resultType
                )
                db.biodataDao().update(updated)
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@IshiharaActivity, ResultActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun getColorBlindnessType(score: Int): String {
        return when {
            score >= 9 -> "Normal"
            score in 5..8 -> "Deuteranopia / Protanopia ringan"
            score in 3..4 -> "Deuteranopia / Protanopia sedang"
            else -> "Deuteranopia / Protanopia parah"
        }
    }
}
