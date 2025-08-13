package com.colorblind.spectra.UI.quiz

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.colorblind.spectra.R
import com.colorblind.spectra.data.lokal.room.AppDatabase
import com.colorblind.spectra.data.model.Answer
import com.colorblind.spectra.data.model.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IshiharaActivity : AppCompatActivity() {

    private enum class Phase { SCREENING, CLASSIFICATION }

    // State
    private var phase: Phase = Phase.SCREENING
    private lateinit var questions: MutableList<Question>
    private var currentIndex = 0
    private val answers = mutableListOf<Answer>()

    // Skor kumulatif
    private var skorNormal = 0
    private var skorDefisiensi = 0
    private var totalSkorDeutan = 0
    private var totalSkorProtan = 0

    // View
    private lateinit var textPlateNumber: TextView
    private lateinit var imagePlate: ImageView
    private lateinit var editAnswer: EditText
    private lateinit var buttonNext: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ishihara)

        textPlateNumber = findViewById(R.id.textPlateNumber)
        imagePlate      = findViewById(R.id.imagePlate)
        editAnswer      = findViewById(R.id.editAnswer)
        buttonNext      = findViewById(R.id.buttonNext)
        progressBar     = findViewById(R.id.progressBar)

        // Mulai hanya dengan plate SCREENING
        questions = getScreeningQuestions().toMutableList()

        showQuestion(currentIndex)

        buttonNext.setOnClickListener {
            val input = editAnswer.text.toString().trim()
            if (input.isEmpty()) {
                editAnswer.error = "Jawaban tidak boleh kosong"
                return@setOnClickListener
            }

            // Simpan jawaban
            val q = questions[currentIndex]
            answers.add(Answer(q, input))

            // Update skor sesuai fase & plate
            if (phase == Phase.SCREENING) {
                updateScreeningScore(q.imageResId, input)
            } else {
                updateClassificationScore(q.imageResId, input, q.correctAnswer)
            }

            // Lanjut
            currentIndex++

            if (currentIndex < questions.size) {
                showLoadingThenNextQuestion()
            } else {
                // Jika fase screening selesai, tentukan apakah perlu klasifikasi
                if (phase == Phase.SCREENING) {
                    if (skorNormal > skorDefisiensi) {
                        // Normal dominan → langsung hasil
                        processResult()
                    } else {
                        // Defisiensi ≥ Normal → tambahkan plate klasifikasi dan lanjut
                        phase = Phase.CLASSIFICATION
                        questions.addAll(getClassificationQuestions())
                        showLoadingThenNextQuestion()
                    }
                } else {
                    // Fase klasifikasi selesai → hasil akhir
                    processResult()
                }
            }
        }
    }

    /* ------------ Daftar pertanyaan ------------ */

    private fun getScreeningQuestions(): List<Question> = listOf(
        Question(R.drawable.plate1,  "12"), // Kontrol
        Question(R.drawable.plate2,  "8"),  // Transformasi
        Question(R.drawable.plate5,  "57"), // Transformasi
        Question(R.drawable.plate7,  "3"),  // Transformasi
        Question(R.drawable.plate10, "2"),  // Menghilang
        Question(R.drawable.plate16, "16"), // Menghilang
        Question(R.drawable.plate18, "-")   // Tersembunyi (jawaban 5 = defisiensi)
    )

    private fun getClassificationQuestions(): List<Question> = listOf(
        Question(R.drawable.plate22, "26"),
        Question(R.drawable.plate23, "42"),
        Question(R.drawable.plate24, "35"),
        Question(R.drawable.plate25, "96")
    )

    /* ------------ UI helpers ------------ */

    private fun showQuestion(index: Int) {
        val q = questions[index]
        textPlateNumber.text = "Plate ${index + 1}"
        imagePlate.setImageResource(q.imageResId)
        editAnswer.text.clear()
        editAnswer.requestFocus()
    }

    private fun showLoadingThenNextQuestion(resetIndex: Boolean = false) {
        progressBar.visibility = View.VISIBLE
        buttonNext.isEnabled = false

        lifecycleScope.launch {
            delay(400)
            progressBar.visibility = View.GONE
            buttonNext.isEnabled = true
            if (resetIndex) currentIndex = 0
            showQuestion(currentIndex)
        }
    }

    /* ------------ Scoring ------------ */

    private fun updateScreeningScore(plateId: Int, input: String) {
        when (plateId) {
            R.drawable.plate1 -> { // Kontrol
                if (input == "12") skorNormal++ else skorDefisiensi++
            }
            R.drawable.plate2 -> { // Transformasi (Normal: 8, Defisiensi Spesifik: 3)
                if (input == "8") {
                    skorNormal++
                } else if (input == "3") {
                    skorDefisiensi++
                }
            }
            R.drawable.plate5 -> { // Transformasi (Normal: 57, Defisiensi Spesifik: 35)
                if (input == "57") {
                    skorNormal++
                } else if (input == "35") {
                    skorDefisiensi++
                }
            }
            R.drawable.plate7 -> { // Transformasi (Normal: 3, Defisiensi Spesifik: 5)
                if (input == "3") {
                    skorNormal++
                } else if (input == "5") {
                    skorDefisiensi++
                }
            }
            R.drawable.plate10 -> { // Menghilang
                if (input == "2") skorNormal++ else skorDefisiensi++
            }
            R.drawable.plate16 -> { // Menghilang
                if (input == "16") skorNormal++ else skorDefisiensi++
            }
            R.drawable.plate18 -> { // Tersembunyi
                if (input == "-") skorNormal++ else skorDefisiensi++
            }
        }
        Log.d(
            "ISHIHARA_DEBUG",
            "SCREENING | PlateID: $plateId | Jawaban: $input | SkorNormal: $skorNormal | SkorDefisiensi: $skorDefisiensi"
        )
    }

    private fun updateClassificationScore(plateId: Int, input: String, correct: String) {
        // Jika pengguna menjawab dengan benar (penglihatan normal), tambah skorNormal dan keluar.
        if (input == correct) {
            skorNormal++
            return
        }

        var isDeficiencyDetected = false

        // Logika klasifikasi baru berdasarkan input spesifik dari pengguna
        when (plateId) {
            R.drawable.plate22 -> { // Normal: 26
                if (input == "6") {
                    totalSkorProtan++
                    isDeficiencyDetected = true
                } else if (input == "2") {
                    totalSkorDeutan++
                    isDeficiencyDetected = true
                }
            }
            R.drawable.plate23 -> { // Normal: 42
                if (input == "2") {
                    totalSkorProtan++
                    isDeficiencyDetected = true
                } else if (input == "4") {
                    totalSkorDeutan++
                    isDeficiencyDetected = true
                }
            }
            R.drawable.plate24 -> { // Normal: 35
                if (input == "5") {
                    totalSkorProtan++
                    isDeficiencyDetected = true
                } else if (input == "3") {
                    totalSkorDeutan++
                    isDeficiencyDetected = true
                }
            }
            R.drawable.plate25 -> { // Normal: 96
                if (input == "6") {
                    totalSkorProtan++
                    isDeficiencyDetected = true
                } else if (input == "9") {
                    totalSkorDeutan++
                    isDeficiencyDetected = true
                }
            }
        }

        // Hanya tambah skor defisiensi jika salah satu jawaban spesifik terdeteksi.
        if (isDeficiencyDetected) {
            skorDefisiensi++
        }

        Log.d(
            "ISHIHARA_DEBUG",
            "CLASSIFICATION | PlateID: $plateId | Jawaban: $input | JawabanBenar: $correct | " +
                    "SkorNormal: $skorNormal | SkorDefisiensi: $skorDefisiensi | Protan: $totalSkorProtan | Deutan: $totalSkorDeutan"
        )
    }
    private fun processResult() {
        val resultType = getColorBlindnessType(
            skorNormal, skorDefisiensi, totalSkorDeutan, totalSkorProtan
        )

        val correctCount = answers.count { it.answer == it.question.correctAnswer }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val latest = db.biodataDao().getLatest()
            if (latest != null) {
                val updated = latest.copy(
                    isIshiharaDone = true,
                    score = correctCount, // total semua jawaban benar
                    hasilTes = resultType,
                    scoreNormal = skorNormal,
                    scoreDeuteranopia = totalSkorDeutan,
                    scoreProtanopia = totalSkorProtan
                )
                db.biodataDao().update(updated)
            }

            withContext(Dispatchers.Main) {
                val intent = Intent(this@IshiharaActivity, ResultActivity::class.java)
                intent.putExtra("RESULT_TYPE", resultType)
                intent.putExtra("SCORE_NORMAL", skorNormal)
                intent.putExtra("SCORE_DEUTERANOPIA", totalSkorDeutan)
                intent.putExtra("SCORE_PROTANOPIA", totalSkorProtan)
                startActivity(intent)
                finish()
            }
        }
    }

    private fun getColorBlindnessType(
        skorNormal: Int,
        skorDefisiensi: Int,
        totalSkorDeutan: Int,
        totalSkorProtan: Int
    ): String {
        return if (skorNormal > skorDefisiensi) {
            "Normal"
        } else {
            if (totalSkorDeutan > totalSkorProtan) "Deuteranopia" else "Protanopia"
        }
    }
}