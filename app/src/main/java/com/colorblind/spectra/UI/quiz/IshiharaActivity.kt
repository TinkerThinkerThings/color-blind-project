package com.colorblind.spectra.UI.quiz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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

        // Load progress kalau ada, kalau tidak mulai baru
        if (!loadProgress()) {
            questions = getScreeningQuestions().toMutableList()
            currentIndex = 0
        }
        showQuestion(currentIndex)

        // Klik tombol Next
        buttonNext.setOnClickListener {
            handleAnswer()
        }

        // Tekan tombol Done/Next dari keyboard
        editAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleAnswer()
                true
            } else false
        }
    }

    /* ------------ Handler Jawaban ------------ */

    private fun handleAnswer() {
        var input = editAnswer.text.toString().trim()
        if (input.isEmpty()) {
            // kalau kosong, otomatis diganti "-"
            input = "-"
        }

        // Tutup keyboard setelah user input
        closeKeyboard()

        val q = questions[currentIndex]
        answers.add(Answer(q, input))

        if (phase == Phase.SCREENING) {
            updateScreeningScore(q.imageResId, input)
        } else {
            updateClassificationScore(q.imageResId, input, q.correctAnswer)
        }

        currentIndex++
        saveProgress() // simpan progress setelah menjawab

        if (currentIndex < questions.size) {
            showLoadingThenNextQuestion()
        } else {
            if (phase == Phase.SCREENING) {
                if (skorNormal > skorDefisiensi) {
                    processResult()
                } else {
                    phase = Phase.CLASSIFICATION
                    questions.addAll(getClassificationQuestions())
                    saveProgress()
                    showLoadingThenNextQuestion()
                }
            } else {
                val sudahAda25 = questions.any { it.imageResId == R.drawable.plate25 }
                if (!sudahAda25 && questions.any { it.imageResId == R.drawable.plate24 }) {
                    if (totalSkorProtan == totalSkorDeutan) {
                        questions.add(Question(R.drawable.plate25, "96"))
                        saveProgress()
                        showLoadingThenNextQuestion()
                    } else {
                        processResult()
                    }
                } else {
                    processResult()
                }
            }
        }
    }

    /* ------------ Daftar pertanyaan ------------ */

    private fun getScreeningQuestions(): List<Question> = listOf(
        Question(R.drawable.plate1,  "12"),
        Question(R.drawable.plate2,  "8"),
        Question(R.drawable.plate5,  "57"),
        Question(R.drawable.plate7,  "3"),
        Question(R.drawable.plate10, "2"),
        Question(R.drawable.plate16, "16"),
        Question(R.drawable.plate18, "-")
    )

    private fun getClassificationQuestions(): List<Question> = listOf(
        Question(R.drawable.plate22, "26"),
        Question(R.drawable.plate23, "42"),
        Question(R.drawable.plate24, "35")
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

    private fun closeKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /* ------------ Scoring ------------ */

    private fun updateScreeningScore(plateId: Int, input: String) {
        when (plateId) {
            R.drawable.plate1 -> if (input == "12") skorNormal++ else skorDefisiensi++
            R.drawable.plate2 -> if (input == "8") skorNormal++ else if (input == "3") skorDefisiensi++
            R.drawable.plate5 -> if (input == "57") skorNormal++ else if (input == "35") skorDefisiensi++
            R.drawable.plate7 -> if (input == "3") skorNormal++ else if (input == "5") skorDefisiensi++
            R.drawable.plate10 -> if (input == "2") skorNormal++ else skorDefisiensi++
            R.drawable.plate16 -> if (input == "16") skorNormal++ else skorDefisiensi++
            R.drawable.plate18 -> if (input == "-") skorNormal++ else skorDefisiensi++
        }
        Log.d("ISHIHARA_DEBUG", "SCREENING | PlateID: $plateId | Jawaban: $input | SkorNormal: $skorNormal | SkorDefisiensi: $skorDefisiensi")
    }

    private fun updateClassificationScore(plateId: Int, input: String, correct: String) {
        if (input == correct) {
            skorNormal++
            return
        }

        var isDeficiencyDetected = false
        when (plateId) {
            R.drawable.plate22 -> {
                if (input == "6") { totalSkorProtan++; isDeficiencyDetected = true }
                else if (input == "2") { totalSkorDeutan++; isDeficiencyDetected = true }
            }
            R.drawable.plate23 -> {
                if (input == "2") { totalSkorProtan++; isDeficiencyDetected = true }
                else if (input == "4") { totalSkorDeutan++; isDeficiencyDetected = true }
            }
            R.drawable.plate24 -> {
                if (input == "5") { totalSkorProtan++; isDeficiencyDetected = true }
                else if (input == "3") { totalSkorDeutan++; isDeficiencyDetected = true }
            }
            R.drawable.plate25 -> {
                if (input == "6") { totalSkorProtan++; isDeficiencyDetected = true }
                else if (input == "9") { totalSkorDeutan++; isDeficiencyDetected = true }
            }
        }

        if (isDeficiencyDetected) skorDefisiensi++

        Log.d(
            "ISHIHARA_DEBUG",
            "CLASSIFICATION | PlateID: $plateId | Jawaban: $input | Benar: $correct | " +
                    "SkorNormal: $skorNormal | SkorDefisiensi: $skorDefisiensi | Protan: $totalSkorProtan | Deutan: $totalSkorDeutan"
        )
    }

    /* ------------ Hasil ------------ */

    private fun processResult() {
        clearProgress() // hapus progress setelah selesai

        val resultType = getColorBlindnessType(skorNormal, skorDefisiensi, totalSkorDeutan, totalSkorProtan)
        val correctCount = answers.count { it.answer == it.question.correctAnswer }

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("IS_IN_ISHIHARA", false).apply()
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            val latest = db.biodataDao().getLatest()
            if (latest != null) {
                val updated = latest.copy(
                    isIshiharaDone = true,
                    score = correctCount,
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

    /* ------------ Progress Save/Load ------------ */

    private fun saveProgress() {
        val prefs = getSharedPreferences("ISHIHARA_PREFS", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("phase", if (phase == Phase.SCREENING) 0 else 1)
            putInt("currentIndex", currentIndex)
            putInt("skorNormal", skorNormal)
            putInt("skorDefisiensi", skorDefisiensi)
            putInt("totalSkorDeutan", totalSkorDeutan)
            putInt("totalSkorProtan", totalSkorProtan)
            apply()
        }
    }

    private fun loadProgress(): Boolean {
        val prefs = getSharedPreferences("ISHIHARA_PREFS", Context.MODE_PRIVATE)
        if (!prefs.contains("currentIndex")) return false

        phase = if (prefs.getInt("phase", 0) == 0) Phase.SCREENING else Phase.CLASSIFICATION
        currentIndex = prefs.getInt("currentIndex", 0)
        skorNormal = prefs.getInt("skorNormal", 0)
        skorDefisiensi = prefs.getInt("skorDefisiensi", 0)
        totalSkorDeutan = prefs.getInt("totalSkorDeutan", 0)
        totalSkorProtan = prefs.getInt("totalSkorProtan", 0)

        questions = if (phase == Phase.SCREENING) {
            getScreeningQuestions().toMutableList()
        } else {
            getScreeningQuestions().toMutableList() + getClassificationQuestions()
        }.toMutableList()

        return true
    }

    private fun clearProgress() {
        val prefs = getSharedPreferences("ISHIHARA_PREFS", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
