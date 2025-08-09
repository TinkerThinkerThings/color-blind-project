package com.colorblind.spectra.data.model

data class Question(
    val imageResId: Int,
    val correctAnswer: String,
    val altAnswers: List<String> = emptyList()
) {
    fun isCorrectAnswer(input: String): Boolean {
        val normalized = input.trim().lowercase()
        return normalized == correctAnswer.lowercase() ||
                altAnswers.any { it.lowercase() == normalized }
    }
}
